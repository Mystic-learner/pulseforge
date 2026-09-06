package com.pulseforge.worker;

import com.pulseforge.model.Metrics;
import com.pulseforge.model.Observation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MetricAggregator {
    private MetricAggregator() {}

    public static Metrics from(List<Observation> observations, Duration window) {
        Metrics metrics = new Metrics();
        if (observations == null || observations.isEmpty()) {
            return metrics;
        }
        List<Double> latencies = new ArrayList<>(observations.size());
        int successCount = 0;
        for (Observation observation : observations) {
            latencies.add(observation.latencyMs());
            if (observation.success()) {
                successCount++;
            }
        }
        latencies.sort(Comparator.naturalOrder());
        int n = latencies.size();
        metrics.setRequestCount(n);
        metrics.setSuccessCount(successCount);
        metrics.setErrorCount(n - successCount);
        metrics.setAvgLatencyMs(latencies.stream().mapToDouble(Double::doubleValue).average().orElseThrow());
        metrics.setP50Ms(percentile(latencies, 0.50));
        metrics.setP95Ms(percentile(latencies, 0.95));
        metrics.setP99Ms(percentile(latencies, 0.99));
        double seconds = window == null ? 0 : window.toMillis() / 1000.0;
        if (seconds <= 0) {
            seconds = 0.001;
        }
        metrics.setThroughput(n / seconds);
        metrics.setErrorRate((double) (n - successCount) / n);
        return metrics;
    }

    public static double percentile(List<Double> sortedAscending, double percentile) {
        if (sortedAscending.isEmpty()) {
            throw new IllegalArgumentException("Cannot compute a percentile with no observations");
        }
        int index = (int) Math.ceil(percentile * sortedAscending.size()) - 1;
        index = Math.max(0, Math.min(sortedAscending.size() - 1, index));
        return sortedAscending.get(index);
    }
}
