package productioneventprocessing.DTOs;

import java.util.List;

public record AckRequest(List<String> eventIds) {}

