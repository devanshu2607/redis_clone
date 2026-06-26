package com.cacheserver.ConC.core.engine;


import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import com.cacheserver.ConC.core.expiry.ExpiryManager;
import com.cacheserver.ConC.core.model.CacheEntry;
import com.cacheserver.ConC.core.store.CacheStore;
import com.cacheserver.ConC.tcp.protocal.CommandParser;
// import com.cacheserver.ConC.tcp.protocal.CommandType;
import com.cacheserver.ConC.tcp.protocal.Command;
import com.cacheserver.ConC.core.config.CacheConfig;
import com.cacheserver.ConC.core.eviction.LruEvictionManager;
// import com.cacheserver.ConC.core.store.CacheStore;
import com.cacheserver.ConC.core.store.InMemoryCacheStore;
// import com.cacheserver.ConC.tcp.server.TcpCacheServer;
import com.cacheserver.ConC.core.util.MemoryEstimator;

public class CacheEngineImpl implements CacheEngine {

    private final CacheStore cacheStore;
    private final InMemoryCacheStore inMemoryCacheStore;
    private final CommandParser commandParser;
    private final ExpiryManager expiryManager;
    private final CacheConfig cacheConfig;
    private final LruEvictionManager lruEvictionManager;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final AtomicLong currentMemoryBytes = new AtomicLong(0);

    public CacheEngineImpl(
            CacheStore cacheStore,
            InMemoryCacheStore inMemoryCacheStore,
            ExpiryManager expiryManager,
            CacheConfig cacheConfig
    ) {
        this.cacheStore = cacheStore;
        this.inMemoryCacheStore = inMemoryCacheStore;
        this.commandParser = new CommandParser();
        this.expiryManager = expiryManager;
        this.cacheConfig = cacheConfig;
        this.lruEvictionManager = new LruEvictionManager();
    }

    @Override
    public String execute(String rawCommand) {
        Command command = commandParser.parse(rawCommand);

        return switch (command.getType()) {
            case PING -> "PONG";
            case SET -> handleSet(command.getArgs());
            case GET -> handleGet(command.getArgs());
            case DEL -> handleDelete(command.getArgs());
            case EXISTS -> handleExists(command.getArgs());
            case TTL -> handleTtl(command.getArgs());
            case UNKNOWN -> "ERR unknown command";
        };
    }

    private String handleSet(String[] args) {
        if (args.length < 2) {
            return "ERR usage: SET key value [EX seconds]";
        }

        String key = args[0];
        String value = args[1];
        long now = System.currentTimeMillis();

        boolean hasTTL = false;
        long expiryTime = 0L;

        if (args.length == 4) {
            if (!"EX".equalsIgnoreCase(args[2])) {
                return "ERR usage: SET key value [EX seconds]";
            }

            long ttlSeconds;
            try {
                ttlSeconds = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                return "ERR invalid TTL";
            }

            if (ttlSeconds <= 0) {
                return "ERR TTL must be greater than 0";
            }

            hasTTL = true;
            expiryTime = now + (ttlSeconds * 1000);
        } else if (args.length != 2) {
            return "ERR usage: SET key value [EX seconds]";
        }

        long estimatedSizeBytes = MemoryEstimator.estimateEntrySize(key, value);

        writeLock.lock();
        try {
            Optional<CacheEntry> existingEntryOptional = cacheStore.get(key);
            long existingSize = existingEntryOptional.map(CacheEntry::getEstimatedSizeBytes).orElse(0L);

            evictIfNeeded(existingSize, estimatedSizeBytes);

            CacheEntry entry = new CacheEntry(value, now, now, expiryTime, hasTTL, estimatedSizeBytes);
            cacheStore.set(key, entry);

            if (hasTTL) {
                expiryManager.add(key, expiryTime);
            }

            long delta = estimatedSizeBytes - existingSize;
            currentMemoryBytes.addAndGet(delta);
        } finally {
            writeLock.unlock();
        }

        return "OK";
    }

    private void evictIfNeeded(long existingSize, long newEntrySize) {
        while (true) {
            boolean overKeyLimit = inMemoryCacheStore.size() >= cacheConfig.getMaxKeys() && existingSize == 0;
            boolean overMemoryLimit =
                    currentMemoryBytes.get() - existingSize + newEntrySize > cacheConfig.getMaxMemoryBytes();

            if (!overKeyLimit && !overMemoryLimit) {
                break;
            }

            Optional<String> lruKeyOptional = lruEvictionManager.findLeastRecentlyUsedKey(inMemoryCacheStore);
            if (lruKeyOptional.isEmpty()) {
                break;
            }

            CacheEntry removedEntry = cacheStore.delete(lruKeyOptional.get());
            if (removedEntry != null) {
                currentMemoryBytes.addAndGet(-removedEntry.getEstimatedSizeBytes());
            }
        }
    }

    private String handleGet(String[] args) {
        if (args.length < 1) {
            return "ERR usage: GET key";
        }

        String key = args[0];
        Optional<CacheEntry> entryOptional = cacheStore.get(key);

        if (entryOptional.isEmpty()) {
            return "(nil)";
        }

        CacheEntry entry = entryOptional.get();
        long now = System.currentTimeMillis();

        if (entry.isExpired(now)) {
            CacheEntry removed = cacheStore.delete(key);
            if (removed != null) {
                currentMemoryBytes.addAndGet(-removed.getEstimatedSizeBytes());
            }
            return "(nil)";
        }

        entry.touch(now);
        return entry.getValue();
    }

    private String handleDelete(String[] args) {
        if (args.length < 1) {
            return "ERR usage: DEL key";
        }

        String key = args[0];
        CacheEntry removed = cacheStore.delete(key);
        if (removed != null) {
            currentMemoryBytes.addAndGet(-removed.getEstimatedSizeBytes());
            return "1";
        }
        return "0";
    }

    private String handleExists(String[] args) {
        if (args.length < 1) {
            return "ERR usage: EXISTS key";
        }

        String key = args[0];
        Optional<CacheEntry> entryOptional = cacheStore.get(key);

        if (entryOptional.isEmpty()) {
            return "0";
        }

        CacheEntry entry = entryOptional.get();
        long now = System.currentTimeMillis();

        if (entry.isExpired(now)) {
            CacheEntry removed = cacheStore.delete(key);
            if (removed != null) {
                currentMemoryBytes.addAndGet(-removed.getEstimatedSizeBytes());
            }
            return "0";
        }

        entry.touch(now);
        return "1";
    }

    private String handleTtl(String[] args) {
        if (args.length < 1) {
            return "ERR usage: TTL key";
        }

        String key = args[0];
        Optional<CacheEntry> entryOptional = cacheStore.get(key);

        if (entryOptional.isEmpty()) {
            return "-2";
        }

        CacheEntry entry = entryOptional.get();
        long now = System.currentTimeMillis();

        if (entry.isExpired(now)) {
            CacheEntry removed = cacheStore.delete(key);
            if (removed != null) {
                currentMemoryBytes.addAndGet(-removed.getEstimatedSizeBytes());
            }
            return "-2";
        }

        if (!entry.hasTTL()) {
            return "-1";
        }

        long ttlMillis = entry.getExpiryTime() - now;
        long ttlSeconds = Math.max(0, ttlMillis / 1000);

        return String.valueOf(ttlSeconds);
    }

    @Override
    public void evictExpired(String key, long expiryTime) {
        writeLock.lock();
        try {
            Optional<CacheEntry> entryOptional = cacheStore.get(key);
            if (entryOptional.isPresent()) {
                CacheEntry entry = entryOptional.get();
                if (entry.hasTTL() && entry.getExpiryTime() == expiryTime) {
                    if (entry.isExpired(System.currentTimeMillis())) {
                        CacheEntry removed = cacheStore.delete(key);
                        if (removed != null) {
                            currentMemoryBytes.addAndGet(-removed.getEstimatedSizeBytes());
                        }
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
}