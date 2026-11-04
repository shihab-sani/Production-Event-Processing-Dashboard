package productioneventprocessing.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "submission_attempts",
        indexes = {
                @Index(name = "idx_attempts_event", columnList = "event_id"),
                @Index(name = "idx_attempts_class", columnList = "classification"),
                @Index(name = "idx_attempts_source", columnList = "source_id")
        })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Column(name = "source_id", length = 64)
    private String sourceId;

    @Column(name = "event_id", length = 128)
    private String eventId;

    @Column(nullable = false, length = 32)
    private String classification; // ACCEPTED | DUPLICATE | CONFLICT | PENDING_REFERENCE | REJECTED

    @Column(name = "error_reason", columnDefinition = "TEXT")
    private String errorReason;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt = Instant.now();

    public static SubmissionAttempt of(String rawJson, String s, String s1, String rejected, String reason) {
        return null;
    }
}
