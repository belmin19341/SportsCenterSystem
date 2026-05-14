# Load-balancer benchmark — two-instances

- Gateway URL: `http://localhost:8080/api/pricing-rules/calculate?facilityId=1&start=2030-06-15T19:00:00&end=2030-06-15T21:00:00`
- Requests requested: 100
- Parallelism: 100
- Successful responses: 100
- Failed responses: 0
- Wall-clock duration: 1264 ms
- Average latency (successful requests): 193.25 ms
- Min latency: 17.24 ms
- Max latency: 772.43 ms
- Throughput: 79.11 req/s
- Unique upstream instances seen: 2

## Upstream distribution

| Instance | Hits | Share |
| --- | ---: | ---: |
| `resource-service@gora.local:8092` | 50 | 50.0% |
| `resource-service@gora.local:8082` | 50 | 50.0% |

## Interpretation

- Requests were distributed across multiple `resource-service` instances, so gateway-level load balancing is active.
- The raw per-request measurements are stored in the adjacent CSV file for deeper analysis or charting.
