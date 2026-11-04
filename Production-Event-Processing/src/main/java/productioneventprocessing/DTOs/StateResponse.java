package productioneventprocessing.DTOs;

import java.util.List;

public record StateResponse(SummaryDto summary,
                            List<PendingEntry> pending,
                            List<ExceptionEntry> exceptions) {
}
