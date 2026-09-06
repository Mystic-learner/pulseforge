package com.pulseforge.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseforge.model.Metrics;
import com.pulseforge.model.Run;
import com.pulseforge.model.RunRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class RunRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final RowMapper<Run> mapper = (rs, rowNum) -> {
        RunRequest request = new RunRequest();
        request.setName(rs.getString("name"));
        request.setUrl(rs.getString("url"));
        request.setMethod(rs.getString("method"));
        request.setUsers(rs.getInt("users"));
        request.setDurationSeconds(rs.getInt("duration_seconds"));
        request.setPattern(rs.getString("pattern"));
        request.setHeaders(readHeaders(rs.getString("headers_json")));
        request.setBody(rs.getString("body"));
        Metrics metrics = new Metrics();
        metrics.setRequestCount(rs.getInt("request_count"));
        metrics.setSuccessCount(rs.getInt("success_count"));
        metrics.setErrorCount(rs.getInt("error_count"));
        metrics.setAvgLatencyMs(getNullableDouble(rs, "avg_latency_ms"));
        metrics.setP50Ms(getNullableDouble(rs, "p50_ms"));
        metrics.setP95Ms(getNullableDouble(rs, "p95_ms"));
        metrics.setP99Ms(getNullableDouble(rs, "p99_ms"));
        metrics.setThroughput(getNullableDouble(rs, "throughput"));
        metrics.setErrorRate(getNullableDouble(rs, "error_rate"));
        Run run = new Run();
        run.setId(rs.getString("id"));
        run.setRequest(request);
        run.setStatus(rs.getString("status"));
        run.setMetrics(metrics);
        Timestamp created = rs.getTimestamp("created_at");
        run.setCreatedAt(created == null ? null : created.toInstant());
        return run;
    };

    public RunRepository(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void insert(Run run) {
        RunRequest request = run.getRequest();
        Metrics metrics = run.getMetrics() == null ? new Metrics() : run.getMetrics();
        jdbc.update(
            """
            INSERT INTO runs (
              id, name, url, method, users, duration_seconds, pattern, headers_json, body, status, created_at,
              request_count, success_count, error_count, avg_latency_ms, p50_ms, p95_ms, p99_ms, throughput, error_rate
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,
            run.getId(),
            request.getName(),
            request.getUrl(),
            request.getMethod(),
            request.getUsers(),
            request.getDurationSeconds(),
            request.getPattern(),
            writeHeaders(request.getHeaders()),
            request.getBody(),
            run.getStatus(),
            Timestamp.from(run.getCreatedAt()),
            metrics.getRequestCount(),
            metrics.getSuccessCount(),
            metrics.getErrorCount(),
            metrics.getAvgLatencyMs(),
            metrics.getP50Ms(),
            metrics.getP95Ms(),
            metrics.getP99Ms(),
            metrics.getThroughput(),
            metrics.getErrorRate()
        );
    }

    public void update(Run run) {
        Metrics metrics = run.getMetrics() == null ? new Metrics() : run.getMetrics();
        jdbc.update(
            """
            UPDATE runs SET
              status=?, request_count=?, success_count=?, error_count=?,
              avg_latency_ms=?, p50_ms=?, p95_ms=?, p99_ms=?, throughput=?, error_rate=?
            WHERE id=?
            """,
            run.getStatus(),
            metrics.getRequestCount(),
            metrics.getSuccessCount(),
            metrics.getErrorCount(),
            metrics.getAvgLatencyMs(),
            metrics.getP50Ms(),
            metrics.getP95Ms(),
            metrics.getP99Ms(),
            metrics.getThroughput(),
            metrics.getErrorRate(),
            run.getId()
        );
    }

    public Optional<Run> findById(String id) {
        List<Run> rows = jdbc.query("SELECT * FROM runs WHERE id = ?", mapper, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<Run> findAllNewestFirst() {
        return jdbc.query("SELECT * FROM runs ORDER BY created_at DESC", mapper);
    }

    private String writeHeaders(Map<String, String> headers) {
        try {
            return objectMapper.writeValueAsString(headers == null ? Map.of() : headers);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private Map<String, String> readHeaders(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private static Double getNullableDouble(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
