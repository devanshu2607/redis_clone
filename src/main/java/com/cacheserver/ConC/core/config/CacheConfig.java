package com.cacheserver.ConC.core.config;

public class CacheConfig {

    private final int port;
    private final int maxKeys;
    private final long cleanerIntervalSeconds;
    private final long maxMemoryBytes;

    public CacheConfig(int port, int maxKeys, long cleanerIntervalSeconds, long maxMemoryBytes) {
        this.port = port;
        this.maxKeys = maxKeys;
        this.cleanerIntervalSeconds = cleanerIntervalSeconds;
        this.maxMemoryBytes = maxMemoryBytes;
    }

    public int getPort() {
        return port;
    }

    public int getMaxKeys() {
        return maxKeys;
    }

    public long getCleanerIntervalSeconds() {
        return cleanerIntervalSeconds;
    }

    public long getMaxMemoryBytes() {
        return maxMemoryBytes;
    }
}