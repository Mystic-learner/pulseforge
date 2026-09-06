package com.pulseforge.model;

public class Metrics {
    private int requestCount;
    private int successCount;
    private int errorCount;
    private Double avgLatencyMs;
    private Double p50Ms;
    private Double p95Ms;
    private Double p99Ms;
    private Double throughput;
    private Double errorRate;

    public Metrics() {}

    public int getRequestCount() { return requestCount; }
    public void setRequestCount(int requestCount) { this.requestCount = requestCount; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public Double getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(Double avgLatencyMs) { this.avgLatencyMs = avgLatencyMs; }

    public Double getP50Ms() { return p50Ms; }
    public void setP50Ms(Double p50Ms) { this.p50Ms = p50Ms; }

    public Double getP95Ms() { return p95Ms; }
    public void setP95Ms(Double p95Ms) { this.p95Ms = p95Ms; }

    public Double getP99Ms() { return p99Ms; }
    public void setP99Ms(Double p99Ms) { this.p99Ms = p99Ms; }

    public Double getThroughput() { return throughput; }
    public void setThroughput(Double throughput) { this.throughput = throughput; }

    public Double getErrorRate() { return errorRate; }
    public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
}
