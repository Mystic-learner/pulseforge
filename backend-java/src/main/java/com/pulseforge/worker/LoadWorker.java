package com.pulseforge.worker;

import com.pulseforge.model.Observation;
import com.pulseforge.model.Run;
import com.pulseforge.model.RunRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class LoadWorker {
    private static final Logger log = LoggerFactory.getLogger(LoadWorker.class);

    private final HttpClient httpClient;
    private final String demoServiceUrl;
    private final Duration requestTimeout;

    public LoadWorker(
        HttpClient httpClient,
        @Value("${demo.service.url}") String demoServiceUrl,
        @Value("${pulseforge.request-timeout-seconds:10}") int requestTimeoutSeconds
    ) {
        this.httpClient = httpClient;
        this.demoServiceUrl = demoServiceUrl;
        this.requestTimeout = Duration.ofSeconds(requestTimeoutSeconds);
    }

    public List<Observation> execute(Run run, Consumer<List<Observation>> onProgress) {
        RunRequest request = run.getRequest();
        URI target = TargetRewriter.resolve(request.getUrl(), demoServiceUrl);
        int users = Math.max(1, request.getUsers());
        int durationSeconds = Math.max(1, request.getDurationSeconds());
        Instant deadline = Instant.now().plusSeconds(durationSeconds);
        ConcurrentLinkedQueue<Observation> observations = new ConcurrentLinkedQueue<>();
        String pattern = request.getPattern() == null ? "baseline" : request.getPattern().toLowerCase();

        log.info("Executing run {} against {} with {} VUs for {}s ({})", run.getId(), target, users, durationSeconds, pattern);

        try (ExecutorService vus = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>(users);
            for (int i = 0; i < users; i++) {
                long delayMs = rampDelayMs(pattern, i, users, durationSeconds);
                futures.add(vus.submit(() -> driveVu(target, request, deadline, delayMs, observations)));
            }
            Instant nextSnapshot = Instant.now().plusSeconds(1);
            while (Instant.now().isBefore(deadline)) {
                sleepQuietly(100);
                if (onProgress != null && Instant.now().isAfter(nextSnapshot)) {
                    onProgress.accept(List.copyOf(observations));
                    nextSnapshot = Instant.now().plusSeconds(1);
                }
            }
            for (Future<?> future : futures) {
                try {
                    future.get(requestTimeout.toMillis() + 2_000, TimeUnit.MILLISECONDS);
                } catch (Exception ignored) {
                    future.cancel(true);
                }
            }
            vus.shutdownNow();
        }
        List<Observation> collected = List.copyOf(observations);
        if (onProgress != null) {
            onProgress.accept(collected);
        }
        log.info("Run {} collected {} observations", run.getId(), collected.size());
        return collected;
    }

    private void driveVu(
        URI target,
        RunRequest request,
        Instant deadline,
        long delayMs,
        ConcurrentLinkedQueue<Observation> observations
    ) {
        if (delayMs > 0) {
            sleepQuietly(delayMs);
        }
        while (Instant.now().isBefore(deadline) && !Thread.currentThread().isInterrupted()) {
            observations.add(send(target, request));
        }
    }

    Observation send(URI target, RunRequest request) {
        String method = request.getMethod() == null ? "GET" : request.getMethod().trim().toUpperCase();
        String body = request.getBody() == null ? "" : request.getBody();
        HttpRequest.BodyPublisher publisher = needsBody(method) && !body.isBlank()
            ? HttpRequest.BodyPublishers.ofString(body)
            : HttpRequest.BodyPublishers.noBody();
        HttpRequest.Builder builder = HttpRequest.newBuilder(target)
            .timeout(requestTimeout)
            .method(method, publisher);
        Map<String, String> headers = request.getHeaders();
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && !name.isBlank() && value != null && isSafeHeader(name)) {
                    builder.header(name, value);
                }
            });
        }
        long started = System.nanoTime();
        try {
            HttpResponse<Void> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
            double latencyMs = (System.nanoTime() - started) / 1_000_000.0;
            int status = response.statusCode();
            boolean success = status >= 200 && status < 300;
            return new Observation(latencyMs, status, success);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            double latencyMs = (System.nanoTime() - started) / 1_000_000.0;
            return new Observation(latencyMs, 0, false);
        } catch (Exception ex) {
            double latencyMs = (System.nanoTime() - started) / 1_000_000.0;
            return new Observation(latencyMs, 0, false);
        }
    }

    static long rampDelayMs(String pattern, int index, int users, int durationSeconds) {
        if (!"ramp-up".equals(pattern) || users <= 1) {
            return 0;
        }
        long windowMs = Math.max(1, (durationSeconds * 1000L) / 2);
        return windowMs * index / (users - 1);
    }

    private static boolean needsBody(String method) {
        return !method.equals("GET") && !method.equals("HEAD") && !method.equals("DELETE");
    }

    private static boolean isSafeHeader(String name) {
        String lower = name.toLowerCase();
        return !lower.equals("host") && !lower.equals("content-length") && !lower.equals("connection");
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
