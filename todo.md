# PulseForge Real-Metrics Remediation

- [x] Audit and remove every hardcoded, random, simulated, or fabricated metric from frontend, backend, reports, seed data, and demo copy.
- [x] Define a nullable real-metrics contract that renders `No data available` when no completed observations exist.
- [x] Implement actual HTTP request execution in the load-test worker against the configured target, including the demo service.
- [x] Capture request start/end timestamps, HTTP status, success/error outcome, and response latency for every executed request.
- [x] Calculate request count, success count, error count, average latency, p50, p95, p99, throughput, and error rate only from captured observations.
- [x] Persist run metadata and timestamped metric samples without manufacturing observations.
- [x] Stream real progress and metric samples to the dashboard over SSE/WebSockets.
- [x] Update reports, comparisons, and AI analysis to consume only real metrics and use `No data available` when appropriate.
- [x] Add tests for percentile calculation, empty data, request validation, worker execution, SSE payloads, and evidence-only analysis.
- [x] Run frontend checks, backend checks, and an end-to-end demo-service execution; Maven + Java 21 are now available in the verification environment.

## Expanded platform requirements

- [x] Add project, target, environment, and safety classification domain contracts with secret redaction boundaries.
- [x] Add OpenAPI JSON/YAML import and URL ingestion contracts with validation errors and manual fallback.
- [x] Add endpoint catalog extraction for methods, parameters, request bodies, response schemas, authentication, and examples.
- [x] Add editable generated workload-plan drafts from imported specifications.
- [x] Add multi-step workload scenarios with variable extraction and `{{variable}}` injection.
- [x] Add per-step assertions, retry, timeout, think time, and failure behavior contracts.
- [x] Add SLO definitions and evaluation against only real collected observations.
- [x] Add trace correlation and OpenTelemetry demo-service boundaries without fabricated spans.
- [x] Add CI/CD threshold enforcement and GitHub Actions example based on real run artifacts.
- [x] Add Testcontainers and React component-test scaffolding for the expanded contracts.
- [x] Update UI navigation and documentation for the expanded developer workflow.

## Standalone release-readiness audit

- [x] Verify every referenced source file, asset URL, favicon path, API path, Docker build context, migration, and CI fixture exists.
- [x] Identify and remove Manus-only frontend runtime imports, environment assumptions, and storage paths from the standalone build, or provide local fallbacks.
- [x] Confirm the frontend can run without the Manus dev server and uses a configurable standalone API base URL.
- [x] Confirm Java backend and demo service build/run commands work from a clean checkout.
- [x] Verify Docker Compose service names, health checks, ports, database migration startup, and asset packaging.
- [x] Run source audits, frontend build/tests, backend tests, demo-service checks, API smoke tests, and final archive integrity checks.
