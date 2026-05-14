# Load-balancing comparison report

## Expected result

- With **two** Resource Service instances, the gateway should distribute traffic across both instances and reduce pressure on any single instance.
- With **one** Resource Service instance, every request should hit the same upstream instance, so there is no balancing benefit to observe.

## Observed result

| Scenario | Requests | Success | Failure | Success rate | Avg latency* | Min | Max | Unique instances |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Two instances | 100 | 100 | 0 | 100.0% | 193.25 ms | 17.24 ms | 772.43 ms | 2 |
| Single instance | 100 | 100 | 0 | 100.0% | 55.01 ms | 18.27 ms | 135.08 ms | 1 |

## Instance distribution

### Two instances

| Instance | Hits | Share |
| --- | ---: | ---: |
| `resource-service@gora.local:8092` | 50 | 50.0% |
| `resource-service@gora.local:8082` | 50 | 50.0% |

### Single instance

| Instance | Hits | Share |
| --- | ---: | ---: |
| `resource-service@gora.local:8082` | 100 | 100.0% |

## Analysis

- The two-instance run proves that gateway-level `lb://resource-service` routing is distributing traffic across multiple Eureka-registered instances.
- The single-instance run behaved as expected: all successful requests stayed on one upstream instance.
- Average latency for successful responses was 251.33% higher with two instances in this local run. Since both scenarios completed all requests, this suggests gateway/discovery overhead, warm-up effects, or local-machine contention outweighed any scaling benefit here.

## Notes

- `Avg latency*` is calculated only from successful (`200`) responses.
- These measurements are environment-dependent; rerunning on another machine or under a different DB/cache warm-up state can change the absolute numbers.
- The adjacent per-scenario CSV files contain the raw per-request measurements if you want to build charts or compute extra statistics.
