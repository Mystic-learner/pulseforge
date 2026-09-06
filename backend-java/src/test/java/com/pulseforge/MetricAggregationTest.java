package com.pulseforge;

import com.pulseforge.model.Metrics;
import com.pulseforge.model.Observation;
import com.pulseforge.worker.MetricAggregator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricAggregationTest {
    @Test
    void emptyObservationsLeaveMetricsUnset() {
        Metrics metrics = MetricAggregator.from(List.of(), Duration.ofSeconds(10));
        assertEquals(0, metrics.getRequestCount());
        assertNull(metrics.getP95Ms());
        assertNull(metrics.getThroughput());
        assertNull(metrics.getErrorRate());
    }

    @Test
    void percentilesAndRatesComeOnlyFromCapturedLatencies() {
        List<Observation> observations = List.of(
            new Observation(10, 200, true),
            new Observation(20, 200, true),
            new Observation(30, 200, true),
            new Observation(40, 500, false),
            new Observation(100, 200, true)
        );
        Metrics metrics = MetricAggregator.from(observations, Duration.ofSeconds(2));
        assertEquals(5, metrics.getRequestCount());
        assertEquals(4, metrics.getSuccessCount());
        assertEquals(1, metrics.getErrorCount());
        assertEquals(40.0, metrics.getAvgLatencyMs());
        assertEquals(30.0, metrics.getP50Ms());
        assertEquals(100.0, metrics.getP95Ms());
        assertEquals(100.0, metrics.getP99Ms());
        assertEquals(2.5, metrics.getThroughput());
        assertEquals(0.2, metrics.getErrorRate());
    }

    @Test
    void percentileRejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> MetricAggregator.percentile(List.of(), 0.95));
    }
}
