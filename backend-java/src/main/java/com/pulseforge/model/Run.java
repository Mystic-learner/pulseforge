package com.pulseforge.model;

import java.time.Instant;

public class Run {
    private String id;
    private RunRequest request;
    private String status;
    private Metrics metrics;
    private Instant createdAt;

    public Run() {}

    public Run(String id, RunRequest request, String status, Metrics metrics, Instant createdAt) {
        this.id = id;
        this.request = request;
        this.status = status;
        this.metrics = metrics;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public RunRequest getRequest() { return request; }
    public void setRequest(RunRequest request) { this.request = request; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Metrics getMetrics() { return metrics; }
    public void setMetrics(Metrics metrics) { this.metrics = metrics; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
