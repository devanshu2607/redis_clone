package com.cacheserver.ConC.ttl;

import java.util.Comparator;
import java.util.PriorityQueue;
import com.cacheserver.ConC.storage.CacheEntry;

public class TTLManager {
    private final PriorityQueue<CacheEntry> expiryQueue = new PriorityQueue<>(
            Comparator.comparingLong(CacheEntry::getExpireAt)
    );

    public void add(CacheEntry entry) {
        if (entry.getExpireAt() > 0) {
            expiryQueue.offer(entry);
        }
    }

    public CacheEntry peek() {
        return expiryQueue.peek();
    }

    public CacheEntry poll() {
        return expiryQueue.poll();
    }

    public boolean isEmpty() {
        return expiryQueue.isEmpty();
    }

    public void clear() {
        expiryQueue.clear();
    }
}
