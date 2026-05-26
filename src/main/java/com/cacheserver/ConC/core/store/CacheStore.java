package com.cacheserver.ConC.core.store;

import java.util.Optional;

import com.cacheserver.ConC.core.model.CacheEntry;

public interface CacheStore {

    void set(String key, CacheEntry entry);

    Optional<CacheEntry> get(String key);

    CacheEntry delete(String key);

    boolean exists(String key);
}