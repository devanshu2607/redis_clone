package com.cacheserver.ConC.core.engine;

public interface CacheEngine {

    String execute(String rawCommand);
    void evictExpired(String key, long expiryTime);
}

