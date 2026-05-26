package com.cacheserver.ConC.core.expiry;

import java.util.Optional;

import com.cacheserver.ConC.core.model.CacheEntry;
import com.cacheserver.ConC.core.store.CacheStore;

public class ExpiryCleaner implements Runnable {

    private final CacheStore cacheStore;
    private final ExpiryManager expiryManager;

    public ExpiryCleaner(CacheStore cacheStore, ExpiryManager expiryManager) {
        this.cacheStore = cacheStore;
        this.expiryManager = expiryManager;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        while (true) {
            ExpiryNode node = expiryManager.peek();

            if (node == null || node.getExpiryTime() > now) {
                break;
            }

            expiryManager.poll();

            Optional<CacheEntry> entryOptional = cacheStore.get(node.getKey());
            if (entryOptional.isEmpty()) {
                continue;
            }

            CacheEntry entry = entryOptional.get();

            if (!entry.hasTTL()) {
                continue;
            }

            if (entry.getExpiryTime() != node.getExpiryTime()) {
                continue;
            }

            if (entry.isExpired(now)) {
                cacheStore.delete(node.getKey());
            }
        }
    }
}