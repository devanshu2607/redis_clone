package com.cacheserver.ConC.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Optional;
import java.util.function.Supplier;

import com.cacheserver.ConC.client.config.CacheClientConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TcpCacheClient implements CacheClient, Closeable {

    private final CacheClientConfig config;
    private final ObjectMapper objectMapper;

    public TcpCacheClient(CacheClientConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void set(String key, String value) {
        set(key, value, config.getDefaultTtlSeconds());
    }

    @Override
    public void set(String key, String value, long ttlSeconds) {
        String response;
        if (ttlSeconds > 0) {
            response = sendCommand("SET " + key + " " + value + " EX " + ttlSeconds);
        } else {
            response = sendCommand("SET " + key + " " + value);
        }

        if (!"OK".equals(response)) {
            throw new CacheClientException("SET failed: " + response);
        }
    }

    @Override
    public Optional<String> get(String key) {
        String response = sendCommand("GET " + key);
        if ("(nil)".equals(response)) {
            return Optional.empty();
        }
        return Optional.of(response);
    }

    @Override
    public boolean delete(String key) {
        return "1".equals(sendCommand("DEL " + key));
    }

    @Override
    public boolean exists(String key) {
        return "1".equals(sendCommand("EXISTS " + key));
    }

    @Override
    public long ttl(String key) {
        String response = sendCommand("TTL " + key);
        try {
            return Long.parseLong(response);
        } catch (NumberFormatException e) {
            throw new CacheClientException("Invalid TTL response: " + response, e);
        }
    }

    @Override
    public <T> void setObject(String key, T value) {
        setObject(key, value, config.getDefaultTtlSeconds());
    }

    @Override
    public <T> void setObject(String key, T value, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(value);
            set(key, json, ttlSeconds);
        } catch (IOException e) {
            throw new CacheClientException("Failed to serialize object for key: " + key, e);
        }
    }

    @Override
    public <T> Optional<T> getObject(String key, Class<T> type) {
        Optional<String> valueOptional = get(key);
        if (valueOptional.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(valueOptional.get(), type));
        } catch (IOException e) {
            throw new CacheClientException("Failed to deserialize object for key: " + key, e);
        }
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader) {
        return getOrLoad(key, type, loader, config.getDefaultTtlSeconds());
    }

    @Override
    public <T> T getOrLoad(String key, Class<T> type, Supplier<T> loader, long ttlSeconds) {
        Optional<T> cached = getObject(key, type);
        if (cached.isPresent()) {
            return cached.get();
        }

        T loaded = loader.get();
        if (loaded != null) {
            setObject(key, loaded, ttlSeconds);
        }
        return loaded;
    }

    private String sendCommand(String command) {
        try (
                Socket socket = new Socket(config.getHost(), config.getPort());
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            reader.readLine();

            writer.write(command);
            writer.newLine();
            writer.flush();

            String response = reader.readLine();
            if (response == null) {
                throw new CacheClientException("No response from cache server");
            }

            return response;
        } catch (IOException e) {
            throw new CacheClientException("Cache server communication failed", e);
        }
    }

    @Override
    public void close() {
    }
}