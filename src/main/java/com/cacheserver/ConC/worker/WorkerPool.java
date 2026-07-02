package com.cacheserver.ConC.worker;

import com.cacheserver.ConC.persistence.PersistenceManager;

public class WorkerPool {
    private final int workerCount;
    private final Worker[] workers;

    public WorkerPool(int workerCount, PersistenceManager persistenceManager) {
        this.workerCount = workerCount;
        this.workers = new Worker[workerCount];
        for (int i = 0; i < workerCount; i++) {
            workers[i] = new Worker(i, persistenceManager);
        }
    }

    public void start() {
        for (Worker worker : workers) {
            worker.start();
        }
    }

    public void shutdown() {
        System.out.println("Shutting down WorkerPool...");
        for (Worker worker : workers) {
            worker.interrupt();
        }
    }

    public Worker getWorker(int index) {
        if (index < 0 || index >= workerCount) {
            throw new IllegalArgumentException("Invalid worker index: " + index);
        }
        return workers[index];
    }

    public int getWorkerCount() {
        return workerCount;
    }
}
