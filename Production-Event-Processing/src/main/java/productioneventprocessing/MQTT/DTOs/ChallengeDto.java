package productioneventprocessing.MQTT.DTOs;

import productioneventprocessing.DTOs.EventPayload;

import java.util.List;

public record ChallengeDto(String protocolVersion,
                           String candidateId,
                           String challengeId,
                           String command,
                           String sentAt,
                           String expiresAt,
                           List<EventPayload> events) {
}
