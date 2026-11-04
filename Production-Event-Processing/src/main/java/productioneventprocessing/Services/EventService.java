package productioneventprocessing.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import productioneventprocessing.DTOs.*;
import productioneventprocessing.Entity.EventStatus;
import productioneventprocessing.Entity.EventType;
import productioneventprocessing.Entity.ProductionEvent;
import productioneventprocessing.Entity.SubmissionAttempt;
import productioneventprocessing.Repositories.ProductionEventRepository;
import productioneventprocessing.Repositories.ProductionSourceRepository;
import productioneventprocessing.Repositories.SubmissionAttemptRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventService {
    private final ProductionEventRepository eventRepo;
    private final SubmissionAttemptRepository attemptRepo;
    private final ProductionSourceRepository sourceRepo;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public ProcessResult processEvent(EventPayload p, String rawJson) {
        // 1. Field-level validation
        ValidationResult v = validate(p);
        if (!v.valid()) {
            attemptRepo.save(SubmissionAttempt.of(rawJson, p.sourceId(), p.eventId(),
                    "REJECTED", v.reason()));
            return new ProcessResult(p.eventId(), "REJECTED", v.reason());
        }

        // 2. Idempotency: same event_id?
        Optional<ProductionEvent> existing = eventRepo.findByEventId(p.eventId());
        if (existing.isPresent()) {
            if (isSameNormalizedData(existing.get(), p)) {
                attemptRepo.save(SubmissionAttempt.of(rawJson, p.sourceId(), p.eventId(),
                        "DUPLICATE", "Identical event already processed"));
                return new ProcessResult(p.eventId(), "DUPLICATE", "Event already processed");
            } else {
                attemptRepo.save(SubmissionAttempt.of(rawJson, p.sourceId(), p.eventId(),
                        "CONFLICT", "event_id already exists with different data"));
                return new ProcessResult(p.eventId(), "CONFLICT", "Conflicting event_id");
            }
        }

        // 3. Register the source on first sight
        sourceRepo.upsert(p.sourceId());

        // 4. Dispatch
        try {
            if ("COUNT".equals(p.type())) {
                return processCount(p, rawJson);
            } else {
                return processVoid(p, rawJson);
            }
        } catch (DataIntegrityViolationException raceLost) {
            // Concurrent insertion of same event_id won → treat as duplicate
            attemptRepo.save(SubmissionAttempt.of(rawJson, p.sourceId(), p.eventId(),
                    "DUPLICATE", "Concurrent duplicate detected"));
            return new ProcessResult(p.eventId(), "DUPLICATE", "Concurrent duplicate");
        }
    }

    /* ---------- COUNT ---------- */

    private ProcessResult processCount(EventPayload p, String rawJson) {
        ProductionEvent count = build(p, EventStatus.PROCESSED);
        eventRepo.save(count);
        attemptRepo.save(SubmissionAttempt.of(rawJson, p.sourceId(), p.eventId(),
                "ACCEPTED", null));
        publisher.publishEvent(new EventAccepted(count));

        resolvePendingVoids(p.sourceId(), p.eventId());

        return new ProcessResult(p.eventId(), "ACCEPTED", "Count processed");
    }

    /* ---------- VOID ---------- */

    private ProcessResult processVoid(EventPayload p, String rawJson) {
        Optional<ProductionEvent> target =
                eventRepo.findByEventIdAndSourceId(p.targetEventId(), p.sourceId());

        if (target.isEmpty()) {
            ProductionEvent pending = build(p, EventStatus.PENDING_REFERENCE);
            eventRepo.save(pending);
            attemptRepo.save(SubmissionAttempt.of(rawJson, p.sourceId(), p.eventId(),
                    "PENDING_REFERENCE", "Target COUNT not found yet"));
            return new ProcessResult(p.eventId(), "PENDING_REFERENCE",
                    "Waiting for target COUNT");
        }

        // Already voided?
        boolean alreadyVoided = eventRepo
                .findBySourceIdAndTypeAndStatusAndTargetEventId(
                        p.sourceId(), EventType.VOID, EventStatus.PROCESSED, p.targetEventId())
                .size() > 0;
        if (alreadyVoided) {
            attemptRepo.save(SubmissionAttempt.of(rawJson, p.sourceId(), p.eventId(),
                    "REJECTED", "COUNT already reversed by another VOID"));
            return new ProcessResult(p.eventId(), "REJECTED", "COUNT already voided");
        }

        ProductionEvent voidEvent = build(p, EventStatus.PROCESSED);
        voidEvent.setAcknowledgedAt(Instant.now()); // auto-ack VOID per workflow rule
        eventRepo.save(voidEvent);
        attemptRepo.save(SubmissionAttempt.of(rawJson, p.sourceId(), p.eventId(),
                "ACCEPTED", null));
        publisher.publishEvent(new VoidResolved(voidEvent, target.get()));

        return new ProcessResult(p.eventId(), "ACCEPTED", "Void applied");
    }

    private void resolvePendingVoids(String sourceId, String countEventId) {
        List<ProductionEvent> pendings =
                eventRepo.findBySourceIdAndTypeAndStatusAndTargetEventId(
                        sourceId, EventType.VOID, EventStatus.PENDING_REFERENCE, countEventId);

        // Order by created_at implicitly from JPA — first stored wins
        pendings.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

        boolean firstWinner = true;
        for (ProductionEvent v : pendings) {
            if (firstWinner) {
                v.setStatus(EventStatus.PROCESSED);
                v.setAcknowledgedAt(Instant.now());
                firstWinner = false;
                attemptRepo.save(SubmissionAttempt.of(
                        toJson(v), v.getSourceId(), v.getEventId(),
                        "ACCEPTED", "Resolved after COUNT arrival"));
                publisher.publishEvent(new VoidResolved(v, null));
            } else {
                attemptRepo.save(SubmissionAttempt.of(
                        toJson(v), v.getSourceId(), v.getEventId(),
                        "REJECTED", "Another pending VOID already reversed this COUNT"));
            }
        }
    }

    /* ---------- Helpers ---------- */

    private ValidationResult validate(EventPayload p) {
        if (p.sourceId() == null || p.sourceId().isBlank())
            return ValidationResult.fail("source_id required");
        if (p.eventId() == null || p.eventId().isBlank())
            return ValidationResult.fail("event_id required");
        if (p.type() == null)
            return ValidationResult.fail("type required");
        if (!"COUNT".equals(p.type()) && !"VOID".equals(p.type()))
            return ValidationResult.fail("type must be COUNT or VOID");
        if (p.eventTime() == null)
            return ValidationResult.fail("event_time required");

        if ("COUNT".equals(p.type())) {
            if (p.quantity() == null || p.quantity() <= 0)
                return ValidationResult.fail("quantity must be a positive integer for COUNT");
            if (p.targetEventId() != null)
                return ValidationResult.fail("target_event_id must be null for COUNT");
        } else {
            if (p.targetEventId() == null || p.targetEventId().isBlank())
                return ValidationResult.fail("target_event_id required for VOID");
            if (p.quantity() != null)
                return ValidationResult.fail("quantity must be null for VOID");
        }
        return ValidationResult.ok();
    }

    private boolean isSameNormalizedData(ProductionEvent e, EventPayload p) {
        return Objects.equals(e.getSourceId(), p.sourceId())
                && Objects.equals(e.getType().name(), p.type())
                && Objects.equals(e.getQuantity(), p.quantity())
                && Objects.equals(e.getTargetEventId(), p.targetEventId())
                && Objects.equals(e.getEventTime(), p.eventTime());
    }

    private ProductionEvent build(EventPayload p, EventStatus status) {
        ProductionEvent e = new ProductionEvent();
        e.setEventId(p.eventId());
        e.setSourceId(p.sourceId());
        e.setType(EventType.valueOf(p.type()));
        e.setQuantity(p.quantity());
        e.setTargetEventId(p.targetEventId());
        e.setEventTime(p.eventTime());
        e.setStatus(status);
        return e;
    }

    private String toJson(ProductionEvent v) {
        return "{\"event_id\":\"" + v.getEventId() + "\",\"source_id\":\"" + v.getSourceId() + "\"}";
    }
}
