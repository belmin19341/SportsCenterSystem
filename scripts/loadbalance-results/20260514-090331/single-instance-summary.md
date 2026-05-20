# Load-balancer benchmark — single-instance

- Gateway URL: `http://localhost:8080/api/pricing-rules/calculate?facilityId=1&start=2030-06-15T19:00:00&end=2030-06-15T21:00:00`
- Requests requested: 100
- Parallelism: 100
- Successful responses: 100
- Failed responses: 0
- Wall-clock duration: 959 ms
- Average latency (successful requests): 55.01 ms
- Min latency: 18.27 ms
- Max latency: 135.08 ms
- Throughput: 104.28 req/s
- Unique upstream instances seen: 1

## Upstream distribution

| Instance | Hits | Share |
| --- | ---: | ---: |
| `resource-service@gora.local:8082` | 100 | 100.0% |

## Interpretation

- Only one upstream instance handled the successful requests, which is expected in the single-instance scenario and a red flag in the two-instance scenario.
- The raw per-request measurements are stored in the adjacent CSV file for deeper analysis or charting.
