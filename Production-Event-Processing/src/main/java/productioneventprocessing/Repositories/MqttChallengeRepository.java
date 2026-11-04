package productioneventprocessing.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import productioneventprocessing.MQTT.Entity.MqttChallenge;

public interface MqttChallengeRepository extends JpaRepository<MqttChallenge, String> {
}
