package com.cacheserver.ConC.core.model;

public class CacheEntry {

    private final String value;
    private final long createdAt;
    private volatile long lastAccessTime;
    private final long expiryTime;
    private final boolean hasTTL;
    private final long estimatedSizeBytes;

    public CacheEntry(
            String value,
            long createdAt,
            long lastAccessTime,
            long expiryTime,
            boolean hasTTL,
            long estimatedSizeBytes
    ) {
        this.value = value;
        this.createdAt = createdAt;
        this.lastAccessTime = lastAccessTime;
        this.expiryTime = expiryTime;
        this.hasTTL = hasTTL;
        this.estimatedSizeBytes = estimatedSizeBytes;
    }

    public String getValue() {
        return value;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public void touch(long now) {
        this.lastAccessTime = now;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean hasTTL() {
        return hasTTL;
    }

    public boolean isExpired(long now) {
        return hasTTL && now >= expiryTime;
    }

    public long getEstimatedSizeBytes() {
        return estimatedSizeBytes;
    }
}