package productioneventprocessing.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "production_events",
        uniqueConstraints = @UniqueConstraint(columnNames = "event_id"),
        indexes = {
                @Index(name = "idx_events_source", columnList = "source_id"),
                @Index(name = "idx_events_status", columnList = "status")
        })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "source_id", nullable = false, length = 64)
    private String sourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private EventType type;

    private Integer quantity;

    @Column(name = "target_event_id", length = 128)
    private String targetEventId;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EventStatus status;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();


}
