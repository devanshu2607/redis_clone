package com.cacheserver.ConC.client;

import java.util.Optional;
import java.util.function.Supplier;

public interface CacheClient {

    void set(String key, String value);

    void set(String key, String value, long ttlSeconds);

    Optional<String> get(String key);

    boolean delete(String key);

    boolean exists(String key);

    long ttl(String key);

    <T> void setObject(String key, T value);

    <T> void setObject(String key, T value, long ttlSeconds);

    <T> Optional<T> getObject(String key, Class<T> type);

    <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader);

    <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, long ttlSeconds);
}