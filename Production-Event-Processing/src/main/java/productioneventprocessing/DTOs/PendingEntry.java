package productioneventprocessing.DTOs;

import java.time.Instant;

public record PendingEntry(String eventId,
                           String sourceId,
                           Integer quantity,
                           Instant eventTime) {
}
