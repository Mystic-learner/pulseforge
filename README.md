# PulseForge

PulseForge is an AI-assisted API reliability and performance testing workspace. It helps developers define controlled HTTP load tests, watch latency and throughput evolve in real time, compare runs, and turn measured signals into explainable recommendations.

## Five-minute demo

1. Install Java 21, Maven, Node 20+, pnpm, and Docker Desktop.
2. Run `docker compose up --build` from the repository root. PostgreSQL, the Spring Boot API, and the intentionally degraded demo service start together.
3. In a second terminal, run `pnpm install && pnpm dev` and open the printed Vite URL.
4. Open **Test configuration**, review the prefilled demo target, choose **Baseline**, and select **Confirm & queue test**.
5. Open **Performance report** to inspect p95/p99, throughput, errors, the rule-based fallback analysis, and the suggested validation test.

The live demo target is `http://localhost:8081/api/checkout`. Other safe, intentionally problematic endpoints are `/api/n-plus-one`, `/api/slow-external`, `/api/inefficient`, and `/api/pool-exhaustion`.

## Repository map

| Area | Location | Responsibility |
|---|---|---|
| Frontend | `client/` | React, TypeScript, Vite, Tailwind, Recharts dashboard |
| API | `backend-java/` | Java 21 / Spring Boot 3 validation, run lifecycle, SSE, worker abstraction |
| Demo target | `demo-service/` | Local-only endpoint fixtures with controlled latency and failures |
| Database | `backend-java/src/main/resources/db/migration/` | PostgreSQL + TimescaleDB schema and seed rows |
| Architecture | `docs/architecture.mmd` | Renderable Mermaid system diagram |
| Tests | `backend-java/src/test/` | Metric and API test extension points |

## API and OpenAPI

The Spring Boot API is served on port 8080. When running, Swagger UI is available at `http://localhost:8080/swagger-ui.html` and the OpenAPI document at `http://localhost:8080/v3/api-docs`. Important endpoints include `GET /api/health`, `GET /api/projects`, `POST /api/runs`, `POST /api/runs/{id}/start?confirm=true`, `GET /api/runs/{id}`, `GET /api/runs/{id}/events`, and `GET /api/runs/{id}/analysis`.

## Safety and design decisions

Test starts require explicit confirmation. The API rejects non-HTTP(S) targets, more than 500 virtual users, or more than 600 seconds. The first MVP uses a single Java virtual-thread worker abstraction instead of Kafka; the boundary can be replaced by a queue-backed worker pool later. The frontend ships with deterministic demo data so the UI remains inspectable without secrets or a hosted dependency.

AI output is deliberately labelled a recommendation. The fallback engine never invents metrics: it evaluates only the structured `Metrics` record produced by the worker and cites p95 latency, error rate, and throughput values in each recommendation. A production deployment should add authentication, outbound target allowlists, persistent metric writes, request-level authorization, and a configured model provider that returns schema-validated JSON.

## Tests

Run frontend type checking and production build with `pnpm check` and `pnpm build`. Run backend tests with `cd backend-java && mvn test`. The project is structured for JUnit and Spring Boot Test coverage of percentile calculation, request validation, lifecycle transitions, and analysis parsing; add a real model adapter only after its contract is pinned with fixtures.

## Environment

Copy `ENVIRONMENT.template` to `.env.example` or `.env` for local overrides. Never commit credentials. Docker Compose supplies the local database values and the demo service latency knobs.

## Limitations and next improvements

The frontend reads run snapshots from the Spring API and shows `No data available` until an actual run has produced observations. The MVP worker executes real HTTP requests with Java 21 virtual threads and derives every metric from captured response timestamps and status codes; it does not invoke JMeter yet. Run metadata, per-request observations, and timestamped aggregate metric snapshots are persisted in PostgreSQL; the migration uses indexed time-series tables and can be converted to a TimescaleDB hypertable in deployments that provide the extension. The next iteration should add a real JMeter adapter, implement user/project authorization, add resource metrics from the demo container, and wire a schema-constrained AI provider.

## Expanded developer workflow

The console now includes project and target boundaries, an OpenAPI intake screen with JSON path validation and manual fallback messaging, an editable multi-step workload draft, SLO policy entry, trace-correlation boundaries, and a CI enforcement example. Workload steps support method, path, expected status, timeout, retries, think time, failure behavior, and response extraction syntax such as `{{auth_token}}` for later requests.

OpenAPI import is intentionally a validation-first preview. It does not execute imported operations, does not persist secrets, and reports a validation error when the supplied document is invalid or has no discoverable paths. The current worker executes the configured single-request load test; multi-step workload execution is represented as an editable plan with step controls and variable syntax, and is the next orchestration extension.

SLO decisions and CI gates are designed to fail closed: no threshold is marked as passing without a completed run containing real observations. Trace correlation similarly displays no data until the demo service exports real OpenTelemetry context associated with request observations. The included GitHub Actions workflow is an example integration and requires Docker plus a CI verification command to be supplied by the deployment environment.
