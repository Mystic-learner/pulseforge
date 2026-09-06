package com.pulseforge;

import com.pulseforge.model.Observation;
import com.pulseforge.model.Run;
import com.pulseforge.model.RunRequest;
import com.pulseforge.worker.LoadWorker;
import com.pulseforge.worker.TargetRewriter;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealLoadWorkerTest {
    @Test
    void executesRealHttpRequestsAndRecordsObservedLatencies() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/checkout", exchange -> {
            hits.incrementAndGet();
            byte[] body = "{\"ok\":true}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
            LoadWorker worker = new LoadWorker(client, "http://127.0.0.1:" + port, 2);
            RunRequest request = new RunRequest();
            request.setUrl("http://127.0.0.1:" + port + "/api/checkout");
            request.setMethod("GET");
            request.setUsers(2);
            request.setDurationSeconds(1);
            request.setPattern("baseline");
            Run run = new Run("run-test", request, "RUNNING", null, Instant.now());
            List<Observation> observations = worker.execute(run, snapshot -> {});
            assertTrue(observations.size() >= 1);
            assertEquals(observations.size(), hits.get());
            assertTrue(observations.stream().allMatch(Observation::success));
            assertTrue(observations.stream().allMatch(o -> o.latencyMs() >= 0));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsNonHttpTargets() {
        assertThrows(IllegalArgumentException.class, () -> TargetRewriter.requireHttp("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> TargetRewriter.requireHttp("javascript:alert(1)"));
    }

    @Test
    void rewritesLoopbackToDemoServiceInsideCompose() {
        URI resolved = TargetRewriter.resolve("http://localhost:8081/api/checkout", "http://demo-service:8081");
        assertEquals("demo-service", resolved.getHost());
        assertEquals("/api/checkout", resolved.getPath());
    }
}
