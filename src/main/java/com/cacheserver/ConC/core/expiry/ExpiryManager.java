package com.cacheserver.ConC.core.expiry;
import java.util.Comparator;
import java.util.PriorityQueue;
public class ExpiryManager {

    private final PriorityQueue<ExpiryNode> expiryQueue =
            new PriorityQueue<>(Comparator.comparingLong(ExpiryNode::getExpiryTime));

    public synchronized void add(String key, long expiryTime) {
        expiryQueue.offer(new ExpiryNode(key, expiryTime));
    }

    public synchronized ExpiryNode peek() {
        return expiryQueue.peek();
    }

    public synchronized ExpiryNode poll() {
        return expiryQueue.poll();
    }

    public synchronized boolean isEmpty() {
        return expiryQueue.isEmpty();
    }
}
