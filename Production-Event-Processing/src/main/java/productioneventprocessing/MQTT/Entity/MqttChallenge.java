package productioneventprocessing.MQTT.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "mqtt_challenges")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MqttChallenge {
    @Id
    @Column(name = "challenge_id", length = 128)
    private String challengeId;

    @Column(name = "request_digest", nullable = false, length = 64)
    private String requestDigest;

    @Column(name = "request_body", nullable = false, columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "result_body", columnDefinition = "TEXT")
    private String resultBody;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
