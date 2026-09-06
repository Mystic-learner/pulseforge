CREATE TABLE IF NOT EXISTS runs (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    method VARCHAR(16) NOT NULL,
    users INT NOT NULL,
    duration_seconds INT NOT NULL,
    pattern VARCHAR(64),
    headers_json TEXT,
    body TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    request_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    error_count INT NOT NULL DEFAULT 0,
    avg_latency_ms DOUBLE PRECISION,
    p50_ms DOUBLE PRECISION,
    p95_ms DOUBLE PRECISION,
    p99_ms DOUBLE PRECISION,
    throughput DOUBLE PRECISION,
    error_rate DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS runs_created_at_idx ON runs (created_at DESC);
