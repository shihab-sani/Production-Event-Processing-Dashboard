package productioneventprocessing.DTOs;

import java.util.List;

public record AckResponse(List<AckResult> results) {}
