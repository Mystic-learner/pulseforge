package com.pulseforge.controller;

import com.pulseforge.model.Run;
import com.pulseforge.model.RunRequest;
import com.pulseforge.service.RunService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RunController {
    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/projects")
    public List<Map<String, Object>> projects() {
        return List.of(Map.of(
            "id", "project-1",
            "name", "Acme Platform",
            "environments", List.of("Local", "Development", "Staging"),
            "targets", List.of(Map.of(
                "id", "checkout-api",
                "name", "Checkout API",
                "url", "http://localhost:8081/api/checkout",
                "safety", "explicit confirmation"
            ))
        ));
    }

    @GetMapping("/runs")
    public List<Run> getRuns() {
        return runService.getAllRuns();
    }

    @PostMapping("/runs")
    public Run createRun(@RequestBody RunRequest request) {
        return runService.createRun(request);
    }

    @PostMapping("/runs/{id}/start")
    public ResponseEntity<Map<String, String>> startRun(
        @PathVariable String id,
        @RequestParam(required = false, defaultValue = "false") boolean confirm
    ) {
        if (!confirm) {
            return ResponseEntity.badRequest().body(Map.of("error", "Confirmation required"));
        }
        runService.startRun(id);
        return ResponseEntity.ok(Map.of("status", "started"));
    }

    @GetMapping("/runs/{id}")
    public ResponseEntity<Run> getRun(@PathVariable String id) {
        Run run = runService.getRun(id);
        if (run == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(run);
    }

    @GetMapping("/runs/{id}/analysis")
    public ResponseEntity<Map<String, Object>> getAnalysis(@PathVariable String id) {
        Map<String, Object> analysis = runService.getAnalysis(id);
        if (analysis == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(analysis);
    }
}
