package com.cacheserver.ConC.client.config;

public class CacheClientConfig {

    private final String host;
    private final int port;
    private final long defaultTtlSeconds;

    public CacheClientConfig(String host, int port, long defaultTtlSeconds) {
        this.host = host;
        this.port = port;
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }
}