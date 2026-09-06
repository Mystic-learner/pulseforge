package com.pulseforge.model;

public record Observation(double latencyMs, int statusCode, boolean success) {}
