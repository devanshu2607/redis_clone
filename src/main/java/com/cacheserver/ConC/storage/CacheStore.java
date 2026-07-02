package com.cacheserver.ConC.storage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class CacheStore {
    private final ConcurrentHashMap<String, CacheEntry> map = new ConcurrentHashMap<>();

    public void set(CacheEntry entry) {
        map.put(entry.getKey(), entry);
    }

    public Optional<CacheEntry> get(String key) {
        return Optional.ofNullable(map.get(key));
    }

    public CacheEntry delete(String key) {
        return map.remove(key);
    }

    public boolean exists(String key) {
        return map.containsKey(key);
    }

    public Map<String, CacheEntry> getMap() {
        return map;
    }
}
