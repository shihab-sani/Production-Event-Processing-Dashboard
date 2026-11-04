package productioneventprocessing.DTOs;

import java.util.List;

public record EventBatchResponse(List<ProcessResult> results) {
}
