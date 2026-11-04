package productioneventprocessing.MQTT.DTOs;

import productioneventprocessing.DTOs.ProcessResult;
import productioneventprocessing.DTOs.SummaryDto;

import java.util.List;

public record ChallengeResponse(String protocolVersion,
                                String candidateId,
                                String challengeId,
                                String status,
                                String processedAt,
                                List<ProcessResult> results,
                                SummaryDto state,
                                String errorCode,
                                String message) {
    public static ChallengeResponse failed(String protocol, String candidate,
                                           String challengeId, String errorCode, String message) {
        return new ChallengeResponse(protocol, candidate, challengeId, "FAILED",
                null, null, null, errorCode, message);
    }
}
