package com.cacheserver.ConC.core.expiry;

public class ExpiryNode {

    private final String key;
    private final long expiryTime;

    public ExpiryNode(String key, long expiryTime) {
        this.key = key;
        this.expiryTime = expiryTime;
    }

    public String getKey() {
        return key;
    }

    public long getExpiryTime() {
        return expiryTime;
    }
}