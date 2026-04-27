package ba.nwt.bookingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo endpoint that proves Spring Cloud LoadBalancer round-robins requests across
 * Resource Service instances registered with Eureka.
 *
 * <p>Internally calls the <em>non-trivial</em> {@code GET /api/pricing-rules/calculate}
 * business endpoint on Resource Service — which executes a transactional EntityGraph
 * query, applies multi-rule pricing multipliers, and returns a PriceQuote — rather than
 * a trivial health/actuator path. Each upstream instance stamps its response with
 * {@code X-Instance-Id} so a client script can tally the distribution.
 */
@RestController
@RequestMapping("/api/lb-demo")
@RequiredArgsConstructor
@Tag(name = "LoadBalancer Demo", description = "Proves Spring Cloud LoadBalancer distribution via pricing-rules/calculate")
public class LoadBalancerDemoController {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RestTemplate loadBalancedRestTemplate;

    /**
     * Calls {@code GET /api/pricing-rules/calculate?facilityId=1&start=...&end=...} on
     * whichever Resource Service instance Eureka resolves, then bubbles back the
     * computed price and upstream instance identity.
     *
     * @param facilityId facility to price (default 1 — always seeded by DataLoader)
     * @param hours      booking duration in hours (default 2)
     */
    @GetMapping("/resource-instance")
    @Operation(summary = "Calls resource-service pricing endpoint via Eureka; returns price quote + upstream instance")
    public ResponseEntity<Map<String, Object>> callResourcePricing(
            @RequestParam(defaultValue = "1") Long facilityId,
            @RequestParam(defaultValue = "2") int hours) {

        LocalDateTime start = LocalDateTime.now().plusDays(1)
                .withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(hours);

        String url = UriComponentsBuilder
                .fromUriString("http://resource-service/api/pricing-rules/calculate")
                .queryParam("facilityId", facilityId)
                .queryParam("start", start.format(ISO))
                .queryParam("end", end.format(ISO))
                .toUriString();

        ResponseEntity<Map> upstream = loadBalancedRestTemplate.getForEntity(url, Map.class);
        String instanceId = upstream.getHeaders().getFirst("X-Instance-Id");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("upstreamInstanceId", instanceId == null ? "unknown" : instanceId);
        body.put("upstreamStatus", upstream.getStatusCode().value());
        body.put("facilityId", facilityId);
        body.put("start", start.format(ISO));
        body.put("end", end.format(ISO));
        if (upstream.getBody() != null) {
            body.put("priceQuote", upstream.getBody());
        }

        return ResponseEntity.ok()
                .header("X-Upstream-Instance", instanceId == null ? "unknown" : instanceId)
                .body(body);
    }
}
