package com.pulseforge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .executor(Executors.newVirtualThreadPerTaskExecutor())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    @Bean(destroyMethod = "close")
    public ExecutorService workerPool() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
