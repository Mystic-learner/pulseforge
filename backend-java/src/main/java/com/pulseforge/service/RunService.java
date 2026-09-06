package com.pulseforge.service;

import com.pulseforge.model.Metrics;
import com.pulseforge.model.Observation;
import com.pulseforge.model.Run;
import com.pulseforge.model.RunRequest;
import com.pulseforge.repository.RunRepository;
import com.pulseforge.worker.LoadWorker;
import com.pulseforge.worker.MetricAggregator;
import com.pulseforge.worker.TargetRewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Service
public class RunService {
    private static final Logger log = LoggerFactory.getLogger(RunService.class);

    private final RunRepository runs;
    private final LoadWorker worker;
    private final ExecutorService workerPool;
    private final int maxUsers;
    private final int maxDurationSeconds;

    public RunService(
        RunRepository runs,
        LoadWorker worker,
        ExecutorService workerPool,
        @Value("${pulseforge.max-users:500}") int maxUsers,
        @Value("${pulseforge.max-duration-seconds:600}") int maxDurationSeconds
    ) {
        this.runs = runs;
        this.worker = worker;
        this.workerPool = workerPool;
        this.maxUsers = maxUsers;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    public List<Run> getAllRuns() {
        return runs.findAllNewestFirst();
    }

    public Run createRun(RunRequest request) {
        validate(request);
        Run run = new Run(
            "run-" + UUID.randomUUID(),
            request,
            "QUEUED",
            new Metrics(),
            Instant.now()
        );
        runs.insert(run);
        return run;
    }

    public void startRun(String id) {
        Run run = runs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
        if ("RUNNING".equals(run.getStatus())) {
            return;
        }
        if ("COMPLETED".equals(run.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Run already completed");
        }
        validate(run.getRequest());
        run.setStatus("RUNNING");
        runs.update(run);
        workerPool.submit(() -> execute(run));
    }

    public Run getRun(String id) {
        return runs.findById(id).orElse(null);
    }

    public Map<String, Object> getAnalysis(String id) {
        Run run = runs.findById(id).orElse(null);
        if (run == null) {
            return null;
        }
        Metrics metrics = run.getMetrics() == null ? new Metrics() : run.getMetrics();
        Map<String, Object> analysis = new LinkedHashMap<>();
        if (metrics.getRequestCount() <= 0) {
            analysis.put("diagnosis", "No data available");
            analysis.put("evidence", List.of());
            return analysis;
        }
        List<String> evidence = new ArrayList<>();
        evidence.add("p95 latency was " + round(metrics.getP95Ms()) + " ms from " + metrics.getRequestCount() + " observed responses");
        evidence.add("error rate was " + round(metrics.getErrorRate() * 100) + "% (" + metrics.getErrorCount() + " errors / " + metrics.getRequestCount() + " requests)");
        evidence.add("throughput was " + round(metrics.getThroughput()) + " req/s over the collected window");
        String diagnosis;
        if (metrics.getErrorRate() != null && metrics.getErrorRate() >= 0.05) {
            diagnosis = "Observed error rate is elevated; inspect failing responses before scaling traffic.";
        } else if (metrics.getP95Ms() != null && metrics.getP95Ms() >= 300) {
            diagnosis = "Observed p95 latency is high relative to a typical interactive API budget.";
        } else {
            diagnosis = "Observed traffic completed with stable error rate and latency for this run.";
        }
        analysis.put("diagnosis", diagnosis);
        analysis.put("evidence", evidence);
        return analysis;
    }

    private void execute(Run run) {
        Instant started = Instant.now();
        try {
            List<Observation> observations = worker.execute(run, snapshot -> persistProgress(run, snapshot, started));
            persistProgress(run, observations, started);
            run.setStatus("COMPLETED");
            runs.update(run);
        } catch (Exception ex) {
            log.warn("Run {} failed: {}", run.getId(), ex.getMessage());
            run.setStatus("FAILED");
            runs.update(run);
        }
    }

    private void persistProgress(Run run, List<Observation> observations, Instant started) {
        Duration window = Duration.between(started, Instant.now());
        if (window.isZero() || window.isNegative()) {
            window = Duration.ofMillis(1);
        }
        run.setMetrics(MetricAggregator.from(observations, window));
        runs.update(run);
    }

    void validate(RunRequest request) {
        TargetRewriter.requireHttp(request.getUrl());
        if (request.getUsers() < 1 || request.getUsers() > maxUsers) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Users must be between 1 and " + maxUsers);
        }
        if (request.getDurationSeconds() < 1 || request.getDurationSeconds() > maxDurationSeconds) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be between 1 and " + maxDurationSeconds + " seconds");
        }
        String method = request.getMethod() == null ? "GET" : request.getMethod().trim().toUpperCase();
        if (!List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD").contains(method)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported HTTP method");
        }
        request.setMethod(method);
        if (request.getName() == null || request.getName().isBlank()) {
            request.setName(method + " " + request.getUrl());
        }
    }

    private static String round(Double value) {
        if (value == null) {
            return "n/a";
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }
}
