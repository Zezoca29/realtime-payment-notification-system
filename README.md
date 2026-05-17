# Realtime Payment Notification System

A **staff-level**, event-driven payment notification system built with Java 21, Spring Boot, Apache Kafka, WebSocket, React, and Docker — demonstrating production-grade distributed systems engineering with full observability, resilience, security, CI/CD, and cloud-native deployment support.

[![CI](https://github.com/Zezoca29/realtime-payment-notification-system/actions/workflows/ci.yml/badge.svg)](https://github.com/Zezoca29/realtime-payment-notification-system/actions/workflows/ci.yml)

---

## Architecture

```
┌─────────────┐   POST /payments   ┌───────────────────────┐
│   React      │ ─────────────────► │   payment-producer    │
│  Dashboard  │                    │   (Spring Boot 3.3)   │
│             │                    │   port 8081            │
│  SockJS +   │                    │                        │
│  STOMP      │                    │  ┌─────────────────┐   │
│             │                    │  │ RateLimitFilter │   │ ← Bucket4j 20 req/s
│  /topic/    │                    │  └────────┬────────┘   │
│  payments   │                    │           │            │
│             │                    │  ┌────────▼────────┐   │
│   port 3000 │                    │  │  PaymentService │   │
└──────┬──────┘                    │  │  @Transactional │   │
       │ WebSocket                 │  └────────┬────────┘   │
       │                           │           │            │
       ▼                           │  ┌────────▼────────┐   │
┌──────────────────┐               │  │  payment_outbox │   │ ← Outbox Pattern
│ websocket-gateway│  HTTP POST    │  │  (PostgreSQL)   │   │
│ (Spring Boot)    │ ◄─────────┐  │  └────────┬────────┘   │
│  port 8083        │           │  │           │ scheduler  │
│                  │           │  │  ┌────────▼────────┐   │
│ InternalApiKey   │           │  │  │ OutboxPublisher │   │
│ Filter (API Key) │           │  │  │  @Scheduled 1s  │   │
│                  │           │  │  └────────┬────────┘   │
│ SecurityConfig   │           │  └───────────┼────────────┘
│ (Spring Security)│           │              │ publishes
└──────────────────┘           │              ▼
       │                       │    ┌──────────────────┐
       │ broadcasts            │    │   Apache Kafka   │
       ▼                       │    │  payment-events  │
┌──────────────────┐           │    │  + retry topics  │
│  React Clients   │           │    │  + DLQ (-dlt)    │
│  (STOMP sub)     │           │    └────────┬──────────┘
└──────────────────┘           │             │ consumes
                               │             ▼
                               │    ┌──────────────────────┐
                               │    │ notification-consumer │
                               │    │  (Spring Boot 3.3)   │
                               │    │                      │
                               │    │ @RetryableTopic      │
                               │    │ 4 attempts + backoff │
                               │    │                      │
                               │    │ Idempotency check    │
                               │    │ CircuitBreaker       │ ← Resilience4j
                               │    │ (Resilience4j)       │
                               │    │  port 8082           │
                               └────┤ /internal/notify     │
                                    └──────────┬───────────┘
                                               │ persists
                                               ▼
                                    ┌──────────────────┐
                                    │   PostgreSQL      │
                                    │  + 8 perf. indexes│
                                    │   port 5432       │
                                    └──────────────────┘

─── Observability ──────────────────────────────────────────────────
 Zipkin  ← distributed traces (all 3 services via Micrometer Brave)
 Prometheus ← metrics scraping /actuator/prometheus
 Grafana  ← pre-built dashboard (latency, error rate, consumer lag)
```

---

## Screenshots

### React Dashboard — Live Event Feed
![Payment Notification Dashboard](docs/dashboard-screenshot.png)
*Real-time payment events pushed via Kafka → WebSocket → STOMP. The dashboard shows live stats, a send-payment form, and an event table with correlation IDs.*

### Kafka UI — Consumer Offsets & Retry Topics
![Kafka UI - Consumer Offsets](docs/kafka-ui-screenshot.png)
*Kafka UI showing the `__consumer_offsets` topic with active consumer groups — including the main `notification-consumer-group` plus its retry groups (`-retry-0`, `-retry-1`, `-retry-2`) and DLT (`-dlt`). Confirms all 4 retry levels are registered and tracking offsets.*

---

## Features

| Feature | Description |
|---|---|
| **Outbox Pattern** | Payment + outbox row saved atomically; relay scheduler publishes to Kafka — eliminates dual-write |
| **Idempotency** | Events deduplicated by `eventId` (app check + DB UNIQUE constraint + race-condition handling) |
| **Rate Limiting** | 20 req/s per IP (burst 40) with Bucket4j — returns `429` + `Retry-After` header |
| **Circuit Breaker** | Resilience4j wraps WebSocket gateway calls — opens after 50% failures, recovers in 30 s |
| **Dead Letter Queue** | After 4 attempts with exponential backoff, events route to `-dlt` topic |
| **Non-blocking Retries** | `@RetryableTopic` — retry partitions never block other messages |
| **API Key Security** | `/internal/**` protected by `X-Internal-API-Key` header; injected via env var |
| **WebSocket Push** | Real-time delivery to React clients via STOMP over SockJS |
| **Distributed Tracing** | Zipkin traces span all 3 services via Micrometer Brave |
| **Prometheus Metrics** | `/actuator/prometheus` on all services; 5 custom outbox metrics |
| **Grafana Dashboard** | Pre-provisioned dashboard: p99 latency, error rate, consumer lag, circuit state, JVM |
| **Correlation ID** | `X-Correlation-ID` propagated via Kafka headers + MDC across all services |
| **RestClient Timeout** | 2 s connect / 5 s read — prevents thread-pool starvation |
| **Testcontainers** | Integration tests with real Kafka + PostgreSQL — no mocks |
| **Frontend Tests** | Vitest suite: StatusBadge, PaymentForm, usePaymentStream hook, paymentApi |
| **CI/CD Pipeline** | GitHub Actions: build → test → Trivy security scan → Docker build validation |
| **Helm Chart** | Full K8s deployment: HPA autoscale, liveness/readiness probes, Ingress, Secrets |
| **Load Testing** | k6 script: smoke / ramp / spike scenarios — SLO: p95 < 500 ms, errors < 1% |
| **Performance Indexes** | 8 PostgreSQL indexes on `customerId`, `status`, `createdAt` — compound query paths |
| **Secrets Management** | `.env` file (gitignored); `.env.example` template; zero hardcoded credentials |

---

## Quick Start

```bash
git clone https://github.com/Zezoca29/realtime-payment-notification-system.git
cd realtime-payment-notification-system

# Copy and configure secrets (edit if needed)
cp .env.example .env

docker compose up --build
```

Services available at:

| Service | URL |
|---|---|
| React Dashboard | http://localhost:3000 |
| Kafka UI | http://localhost:8080 |
| Payment Producer API | http://localhost:8081 |
| Payment Producer Swagger | http://localhost:8081/swagger-ui.html |
| Notification Consumer | http://localhost:8082 |
| Notification Consumer Swagger | http://localhost:8082/swagger-ui.html |
| WebSocket Gateway | http://localhost:8083 |
| **Prometheus** | **http://localhost:9090** |
| **Grafana** | **http://localhost:3001** (admin/admin) |
| **Zipkin** | **http://localhost:9411** |
| PostgreSQL | localhost:5432 |

---

## REST API Reference

### POST a Payment
```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "X-Correlation-ID: my-trace-id-001" \
  -d '{
    "customerId": "customer-001",
    "amount": 250.00,
    "currency": "BRL",
    "status": "APPROVED"
  }'
```

Response:
```json
{
  "paymentId": "a3f8b2c1-...",
  "correlationId": "my-trace-id-001",
  "status": "APPROVED",
  "message": "Payment event published to Kafka"
}
```

### Query Payments (paginated)
```bash
curl "http://localhost:8081/api/v1/payments?page=0&size=20"
curl "http://localhost:8081/api/v1/payments/{paymentId}"
```

### Query Notifications (paginated)
```bash
curl "http://localhost:8082/api/v1/notifications?page=0&size=20"
curl "http://localhost:8082/api/v1/notifications/customer/{customerId}"
curl "http://localhost:8082/api/v1/notifications/status/APPROVED"
curl "http://localhost:8082/api/v1/notifications/{eventId}"
```

> Full interactive API documentation available at the Swagger UI links above.

The event flows:
1. API persists payment + outbox row in one DB transaction (Outbox Pattern)
2. Scheduler reads outbox → publishes to `payment-events`
3. Consumer reads, checks idempotency, persists notification
4. Consumer calls WebSocket Gateway (protected by API key, wrapped by circuit breaker)
5. Gateway broadcasts to all connected React clients via STOMP

---

## Outbox Pattern

```
POST /payments
       │
       ▼
┌─────────────────────────────────────────┐
│  @Transactional                          │
│  ┌───────────────┐  ┌─────────────────┐ │
│  │ payments table│  │ payment_outbox  │ │
│  │  INSERT       │  │  INSERT PENDING │ │
│  └───────────────┘  └─────────────────┘ │
└─────────────────────────────────────────┘
       │ commit (both or neither)
       ▼
┌─────────────────────────────────────────┐
│  OutboxEventPublisher  @Scheduled(1s)   │
│                                          │
│  SELECT * WHERE status=PENDING LIMIT 50 │
│       │                                  │
│  ┌────▼──────┐  success → status=SENT   │
│  │  Kafka    │  failure → retryCount++  │
│  │  publish  │  after 5 fails → FAILED  │
│  └───────────┘                          │
└─────────────────────────────────────────┘
```

**Why this matters:** Without outbox, if the app crashes after saving the payment but before publishing to Kafka, the message is lost forever. With outbox, the message will be picked up on the next scheduler tick — even after a restart.

---

## Rate Limiting

```
POST /api/**
       │
       ▼
 RateLimitFilter (OncePerRequestFilter, highest precedence)
       │
  bucket = getOrCreate(ip)  ← ConcurrentHashMap per IP
       │
  tryConsume(1 token)?
       │
  YES ──┘── NO
   │         │
proceed   HTTP 429
         Retry-After: 3
         {"error": "Rate limit exceeded"}
```

Steady-state: **20 req/s per IP**. Burst: **40 tokens**. For multi-instance deployments, replace with Redis-backed Bucket4j Spring Boot Starter.

---

## Circuit Breaker

```
notification-consumer → WebSocket Gateway call
         │
  @CircuitBreaker(name="websocket-gateway")
         │
    ┌────▼─────────────────────┐
    │  CLOSED (normal)         │
    │  sliding window: 10 calls│
    │  failure threshold: 50%  │
    └────┬─────────────────────┘
         │ >50% failures
         ▼
    ┌────────────────────────┐
    │  OPEN (30 s)           │
    │  fallback: log + skip  │ ← event already in DB, safe to skip
    └────┬───────────────────┘
         │ after 30 s
         ▼
    ┌────────────────────────┐
    │  HALF-OPEN             │
    │  3 probe calls         │
    │  success? → CLOSED     │
    └────────────────────────┘
```

---

## Idempotency Design

```
Kafka delivers event (at-least-once guarantee)
          │
          ▼
  existsByEventId(eventId)?
          │
    YES ──┘── NO
     │         │
  SKIP    persist + broadcast
  (log warn)
```

The `eventId` is indexed with a `UNIQUE` constraint on the database.  
Even under race conditions, a `DataIntegrityViolationException` is caught and swallowed gracefully.

**Why this matters:** Kafka guarantees _at-least-once_ delivery. Without idempotency, a broker rebalance or consumer crash would process the same payment twice — charging customers double.

---

## DLQ & Retry Strategy

```
payment-events
    │
    ├── failure → payment-events-retry-0  (5s delay)
    │                   │
    │             failure → payment-events-retry-1  (15s delay)
    │                             │
    │                       failure → payment-events-retry-2  (30s delay)
    │                                         │
    │                                   failure → payment-events-dlt
    │                                              (Dead Letter Topic)
    ▼
 success → persist + notify
```

DLT events are logged at ERROR level and can be routed to alerting (PagerDuty, Slack) without code changes.

**Architectural decision:** We use `@RetryableTopic` (Spring Kafka non-blocking retries) instead of blocking `RetryTemplate`. This prevents the consumer thread from stalling and allows other partitions to continue processing during retry delays.

---

## Kafka vs RabbitMQ — Architectural Decision

| Dimension | Kafka | RabbitMQ |
|---|---|---|
| **Ordering** | Per-partition guarantee | Queue-level, weaker |
| **Replay** | Yes — consumer can re-read from offset | No |
| **Throughput** | Millions/sec (log-structured) | Moderate |
| **Retention** | Configurable (days/weeks) | Until consumed |
| **DLQ** | Native retry topics | Dead letter exchange |
| **Use case fit** | Event sourcing, audit trail | Task queues, RPC |

Kafka was chosen because payment events require **audit replay**, **ordering per payment**, and **high throughput** during peak hours.

---

## Exactly-Once vs At-Least-Once

This system uses **at-least-once delivery** (Kafka default) + **application-level idempotency**:

- Producer: `enable.idempotence=true`, `acks=all` — prevents broker-level duplicates
- Consumer: `eventId` deduplication — prevents application-level duplicates

True Kafka exactly-once (`isolation.level=read_committed` + transactions) adds operational complexity and was deferred as a V2 enhancement.

---

## Running Tests

```bash
# Shared kernel (required first)
cd backend/shared-kernel && mvn install

# Payment producer integration tests (Testcontainers: real Kafka + PostgreSQL)
cd backend/payment-producer && mvn test

# Notification consumer idempotency tests
cd backend/notification-consumer && mvn test

# Frontend unit tests (Vitest)
cd frontend/dashboard-react
npm ci
npm test             # run once
npm run test:watch   # watch mode
npm run test:coverage  # with coverage report (threshold: 70%)
```

Tests spin up real Kafka and PostgreSQL containers on the backend — no mocking of infrastructure.

### Load Testing (k6)

```bash
# Requires k6 installed and the stack running
k6 run infra/k6/load-test.js

# Scenarios: smoke (2 VUs/30 s) → ramp (0→50 VUs) → spike (200 VUs burst)
# SLOs: p95 < 500 ms, error rate < 1%
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (Virtual Threads ready) |
| Framework | Spring Boot 3.3 |
| Messaging | Apache Kafka + Spring Kafka |
| Persistence | Spring Data JPA + PostgreSQL 16 + 8 perf. indexes |
| Real-time | Spring WebSocket + STOMP + SockJS |
| HTTP Client | Spring RestClient + Apache HttpClient5 (pool + timeouts) |
| Resilience | Resilience4j (circuit breaker) + Bucket4j (rate limiting) |
| Security | Spring Security + internal API key filter |
| Tracing | Micrometer Brave + Zipkin |
| Metrics | Micrometer + Prometheus + Grafana |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Error Handling | RFC 7807 ProblemDetail |
| Testing (backend) | JUnit 5 + Testcontainers + Awaitility |
| Testing (frontend) | Vitest + Testing Library + jsdom + msw |
| Frontend | React 18 + Vite + TailwindCSS + Recharts |
| CI/CD | GitHub Actions (test → Trivy scan → Docker build) |
| Kubernetes | Helm chart (Deployments, HPA, Ingress, Secrets, ConfigMap) |
| Load Testing | k6 (smoke / ramp / spike scenarios) |
| Infra | Docker Compose + Nginx |

---

## Project Structure

```
realtime-payment-notification-system/
│
├── .github/
│   └── workflows/
│       └── ci.yml              # CI: test → Trivy scan → Docker build
│
├── backend/
│   ├── shared-kernel/          # Shared DTOs and domain events
│   ├── payment-producer/       # REST API → Outbox → Kafka producer
│   │   └── filter/             # RateLimitFilter (Bucket4j)
│   │   └── scheduler/          # OutboxEventPublisher (@Scheduled)
│   ├── notification-consumer/  # Kafka consumer — idempotency + circuit breaker + DLQ
│   └── websocket-gateway/      # STOMP broadcast server + InternalApiKeyFilter
│
├── frontend/
│   └── dashboard-react/        # Live event dashboard (React + Vite)
│       └── src/test/           # Vitest test suites
│
├── helm/                       # Kubernetes Helm chart
│   └── templates/              # Deployments, HPA, Ingress, Services, Secrets
│
├── infra/
│   ├── k6/
│   │   └── load-test.js        # Smoke / ramp / spike load scenarios
│   ├── grafana/
│   │   ├── provisioning/       # Auto-provisioned datasource + dashboard provider
│   │   └── dashboards/         # payments-dashboard.json
│   ├── prometheus/
│   │   └── prometheus.yml      # Scrape config for all 3 backend services
│   └── postgres/
│       └── init.sql            # Schema + 8 performance indexes
│
├── .env.example                # Secret template (commit this, not .env)
├── docker-compose.yml          # Full local stack (includes Prometheus, Grafana, Zipkin)
└── README.md
```

---

## Roadmap

### V2
- [ ] Redis cache for hot idempotency checks (sub-millisecond)
- [ ] JWT authentication on WebSocket handshake
- [ ] Saga Pattern for multi-step payment workflows
- [ ] CQRS read model for analytics queries
- [ ] Redis-backed Bucket4j for multi-instance rate limiting

### V3
- [ ] Kafka Streams for real-time aggregation and fraud detection
- [ ] Contract testing with Pact
- [ ] OpenTelemetry OTEL collector (replace Zipkin direct export)

### ~~V1 — Completed~~
- [x] Outbox Pattern (transactional guarantee between DB and Kafka)
- [x] Rate Limiting with Bucket4j (per-IP token bucket)
- [x] Circuit Breaker with Resilience4j
- [x] Internal API Key security on WebSocket Gateway
- [x] Prometheus + Grafana observability
- [x] Distributed tracing with Zipkin
- [x] Frontend unit tests (Vitest — 22+ test cases)
- [x] GitHub Actions CI/CD with Trivy security scan
- [x] Helm chart for Kubernetes (HPA + Ingress + probes)
- [x] k6 load testing with SLO thresholds
- [x] 8 PostgreSQL performance indexes
- [x] Secrets management via .env (zero hardcoded credentials)

---

## Author

Built by **Zez Technology** — demonstrating staff-level event-driven architecture using 100% free, open-source tooling.

Production patterns covered: Outbox, Idempotency, DLQ, Circuit Breaker, Rate Limiting, Distributed Tracing, Secrets Management, CI/CD, Helm/K8s, Load Testing.
