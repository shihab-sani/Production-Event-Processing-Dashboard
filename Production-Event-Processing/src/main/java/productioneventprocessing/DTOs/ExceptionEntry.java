package productioneventprocessing.DTOs;

import java.time.Instant;

public record ExceptionEntry(String eventId,
                             String sourceId,
                             String type,
                             Integer quantity,
                             String targetEventId,
                             Instant eventTime,
                             String status,
                             String reason) {
}
