package productioneventprocessing.DTOs;

public record SummaryDto(long netTotal,
                         long processedEvents,
                         long pendingAck,
                         long unresolved,
                         long duplicates,
                         long conflicts) {
}
