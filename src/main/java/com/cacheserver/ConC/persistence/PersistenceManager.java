package com.cacheserver.ConC.persistence;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.cacheserver.ConC.command.Command;

public class PersistenceManager {
    private final BlockingQueue<Command> queue = new LinkedBlockingQueue<>();
    private final String aofPath;
    private AOFWriter writerThread;

    public PersistenceManager(String aofPath) {
        this.aofPath = aofPath;
    }

    public void logWrite(Command command) {
        queue.offer(command);
    }

    public void start() {
        writerThread = new AOFWriter(queue, aofPath);
        writerThread.start();
    }

    public void shutdown() {
        if (writerThread != null) {
            writerThread.shutdown();
        }
    }

    public String getAofPath() {
        return aofPath;
    }
}
