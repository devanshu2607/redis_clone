package com.cacheserver.ConC.storage;

public class CacheEntry {
    private final String key;
    private final String value;
    private final long expireAt;

    public CacheEntry(String key, String value, long expireAt) {
        this.key = key;
        this.value = value;
        this.expireAt = expireAt;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public long getExpireAt() {
        return expireAt;
    }

    public boolean isExpired() {
        return expireAt > 0 && System.currentTimeMillis() >= expireAt;
    }
}
