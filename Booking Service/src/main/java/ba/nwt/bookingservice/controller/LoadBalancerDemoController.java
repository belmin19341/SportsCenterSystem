package ba.nwt.bookingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo endpoint that proves Spring Cloud LoadBalancer round-robins requests across
 * Resource Service instances registered with Eureka. Each call surfaces the
 * X-Instance-Id header set by the upstream so a client script can tally distribution.
 */
@RestController
@RequestMapping("/api/lb-demo")
@RequiredArgsConstructor
@Tag(name = "LoadBalancer Demo", description = "Proves Spring Cloud LoadBalancer distribution")
public class LoadBalancerDemoController {

    private final RestTemplate loadBalancedRestTemplate;

    @GetMapping("/resource-instance")
    @Operation(summary = "Calls resource-service via Eureka and returns its X-Instance-Id")
    public ResponseEntity<Map<String, String>> callResource() {
        ResponseEntity<String> upstream =
                loadBalancedRestTemplate.getForEntity("http://resource-service/actuator/health", String.class);
        String instanceId = upstream.getHeaders().getFirst("X-Instance-Id");
        Map<String, String> body = new HashMap<>();
        body.put("upstreamInstanceId", instanceId == null ? "unknown" : instanceId);
        body.put("upstreamStatus", String.valueOf(upstream.getStatusCode().value()));
        return ResponseEntity.ok().header("X-Upstream-Instance", instanceId == null ? "unknown" : instanceId).body(body);
    }
}
