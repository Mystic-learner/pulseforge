package com.pulseforge.worker;

import java.net.URI;

public final class TargetRewriter {
    private TargetRewriter() {}

    public static URI requireHttp(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("A target URL is required");
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Only HTTP(S) targets are allowed");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only HTTP(S) targets are allowed");
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("Target URL must include a host");
        }
        return uri;
    }

    /**
     * When the API runs in Docker, the browser still posts http://localhost:8081.
     * Rewrite loopback hosts to the demo service hostname inside the compose network.
     */
    public static URI resolve(String url, String demoServiceUrl) {
        URI target = requireHttp(url);
        if (demoServiceUrl == null || demoServiceUrl.isBlank()) {
            return target;
        }
        URI demo = URI.create(demoServiceUrl);
        if (!isLoopback(target.getHost()) || isLoopback(demo.getHost())) {
            return target;
        }
        int port = demo.getPort() > 0 ? demo.getPort() : target.getPort();
        try {
            return new URI(
                demo.getScheme(),
                null,
                demo.getHost(),
                port,
                target.getPath(),
                target.getQuery(),
                target.getFragment()
            );
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not resolve demo target", ex);
        }
    }

    static boolean isLoopback(String host) {
        if (host == null) {
            return false;
        }
        return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1");
    }
}
