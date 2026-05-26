package com.cacheserver.ConC.core.eviction;

import java.util.Map;
import java.util.Optional;

import com.cacheserver.ConC.core.model.CacheEntry;
import com.cacheserver.ConC.core.store.InMemoryCacheStore;

public class LruEvictionManager {

    public Optional<String> findLeastRecentlyUsedKey(InMemoryCacheStore cacheStore) {
        String lruKey = null;
        long oldestAccessTime = Long.MAX_VALUE;

        for (Map.Entry<String, CacheEntry> entry : cacheStore.snapshot().entrySet()) {
            CacheEntry cacheEntry = entry.getValue();

            if (cacheEntry.getLastAccessTime() < oldestAccessTime) {
                oldestAccessTime = cacheEntry.getLastAccessTime();
                lruKey = entry.getKey();
            }
        }

        return Optional.ofNullable(lruKey);
    }
}