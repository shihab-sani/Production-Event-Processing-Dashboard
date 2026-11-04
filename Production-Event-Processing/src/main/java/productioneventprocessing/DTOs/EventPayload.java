package productioneventprocessing.DTOs;

import java.time.Instant;

public record EventPayload(String sourceId,
                           String eventId,
                           String type,
                           Integer quantity,
                           String targetEventId,
                           Instant eventTime) {
}
