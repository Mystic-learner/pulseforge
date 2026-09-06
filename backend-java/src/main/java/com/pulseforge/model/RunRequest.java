package com.pulseforge.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.LinkedHashMap;
import java.util.Map;

public class RunRequest {
    private String name;
    private String url;
    private String method = "GET";
    private int users = 5;
    private int durationSeconds = 10;
    private String pattern = "baseline";
    @JsonDeserialize(using = HeadersDeserializer.class)
    private Map<String, String> headers = new LinkedHashMap<>();
    private String body = "";

    public RunRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public int getUsers() { return users; }
    public void setUsers(int users) { this.users = users; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers == null ? new LinkedHashMap<>() : headers;
    }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
