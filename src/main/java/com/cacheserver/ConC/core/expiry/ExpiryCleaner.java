package com.cacheserver.ConC.core.expiry;

import com.cacheserver.ConC.core.engine.CacheEngine;

public class ExpiryCleaner implements Runnable {

    private final CacheEngine cacheEngine;
    private final ExpiryManager expiryManager;

    public ExpiryCleaner(CacheEngine cacheEngine, ExpiryManager expiryManager) {
        this.cacheEngine = cacheEngine;
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
            cacheEngine.evictExpired(node.getKey(), node.getExpiryTime());
        }
    }
}