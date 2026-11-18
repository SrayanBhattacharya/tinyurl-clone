# k6 Load-Test Results (no-cache baseline)

## Machine Setup
- **Date**: 2026-11-18 08:33:46 IST
- **CPU**: AMD Ryzen 5 5600H with Radeon Graphics (6 cores, 12 threads)
- **RAM**: 7.4 GiB total (2.7 GiB free, 4.8 GiB available)
- **WSL2 Version**: WSL 3.0.5.0 (Kernel 6.18.58.1-microsoft-standard-WSL2, Fedora Linux 44)
- **Java Version**: OpenJDK 25.0.4.1 (build 25.0.4.1+1)
- **Postgres Image Tag**: `postgres:18-alpine`
- **Redis Image Tag**: `redis:8-alpine`

---

## Load Test Results Table

| Reads/s target | Writes/s target | Actual http_reqs/s | Read p95 | Read p99 | Create p95 | Create p99 | Error Rate | Dropped Iterations | Pass/Fail |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| 500 | 30 | 529.27 | 2.39ms | 3.43ms | 2.80ms | 4.47ms | 0.00% | 0 | PASS |
| 1000 | 60 | 1055.73 | 3.41ms | 4.49ms | 3.12ms | 4.74ms | 0.00% | 0 | PASS |
| 2000 | 120 | 1720.58 | 499.22ms | 622.59ms | 517.57ms | 630.92ms | 0.00% | 22577 | FAIL |
| 3805 | 228 | n/a | n/a | n/a | n/a | n/a | n/a | n/a | SKIPPED |

*Note: Checks passed at 100.00% across all executed runs. The run at (2000, 120) failed thresholds on `http_req_duration{scenario:redirects}` (p95 reached 499.22ms vs limit <100ms) and `http_req_duration{scenario:creates}` (p95 reached 517.57ms vs limit <200ms) with 22,577 dropped iterations.*

---

## Performance Notes
- **Highest rate passing all thresholds**: Target `(READ_RATE=1000, WRITE_RATE=60)` achieving 1055.73 actual reqs/s with Read p95 of 3.41ms, Create p95 of 3.12ms, 0.00% errors, and 0 dropped iterations.
- **Degradation point**: Degradation between 1000/60 and 2000/120 req/s is not CPU- or RAM-bound (~75% CPU idle, 2.4/7.4 GiB RAM, no swap during the run). `pg_stat_activity` during the run showed write connections waiting on WAL (WALWrite/WalSync): slow commit fsync on WSL2 keeps creates holding the 10-connection Hikari pool, so redirect reads queue behind them. Single snapshot; hypothesis, not proven. Redis should relieve the read side only. Per the ramp stop criteria, the ramp was halted early and `(3805, 228)` was skipped.

---

## Caveats
- Everything ran on one laptop, so the app, Postgres, Redis, and k6 compete for the same CPU and memory resources.
- Only 200 distinct keys are read, so Postgres serves them directly from memory/buffer cache, which flatters the no-cache baseline.
- Label: **no-cache baseline**.
