package productioneventprocessing.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import productioneventprocessing.DTOs.EventBatchResponse;
import productioneventprocessing.DTOs.EventPayload;
import productioneventprocessing.DTOs.ProcessResult;
import productioneventprocessing.Services.EventService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;
    private final ObjectMapper mapper;

    @PostMapping("/api/events")
    public ResponseEntity<EventBatchResponse> post(@RequestBody JsonNode body) {
        List<JsonNode> items = new ArrayList<>();
        if (body.isArray()) {
            body.forEach(items::add);
        } else if (body.isObject()) {
            items.add(body);
        } else {
            return ResponseEntity.badRequest().build();
        }

        List<ProcessResult> results = new ArrayList<>();
        for (JsonNode node : items) {
            try {
                EventPayload payload = mapper.treeToValue(node, EventPayload.class);
                results.add(eventService.processEvent(payload, node.toString()));
            } catch (Exception e) {
                // Parse failure — still record attempt, reject item
                String eventId = node.path("event_id").asText(null);
                String sourceId = node.path("source_id").asText(null);
                ProcessResult rejected = new ProcessResult(eventId, "REJECTED",
                        "Parse error: " + e.getMessage());
                results.add(rejected);
                // Note: record via a dedicated repository call if you prefer
            }
        }
        return ResponseEntity.ok(new EventBatchResponse(results));
    }
}
