package com.cacheserver.ConC.core.store;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.cacheserver.ConC.core.model.CacheEntry;

public class InMemoryCacheStore implements CacheStore {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public void set(String key, CacheEntry entry) {
        cache.put(key, entry);
    }

    @Override
    public Optional<CacheEntry> get(String key) {
        return Optional.ofNullable(cache.get(key));
    }

    @Override
    public CacheEntry delete(String key) {
        return cache.remove(key);
    }

    @Override
    public boolean exists(String key) {
        return cache.containsKey(key);
    }

    public int size() {
        return cache.size();
    }

    public Map<String, CacheEntry> snapshot() {
        return Map.copyOf(cache);
    }
}