package com.cacheserver.ConC.worker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.cacheserver.ConC.command.Command;
import com.cacheserver.ConC.command.CommandType;
import com.cacheserver.ConC.persistence.PersistenceManager;
import com.cacheserver.ConC.storage.CacheEntry;
import com.cacheserver.ConC.storage.CacheStore;
import com.cacheserver.ConC.ttl.TTLManager;

public class Worker extends Thread {
    private final int workerId;
    private final BlockingQueue<WorkerTask> taskQueue = new LinkedBlockingQueue<>();
    private final CacheStore store = new CacheStore();
    private final TTLManager ttlManager = new TTLManager();
    private final PersistenceManager persistenceManager;

    public static class WorkerTask {
        private final Command command;
        private final SocketChannel channel;

        public WorkerTask(Command command, SocketChannel channel) {
            this.command = command;
            this.channel = channel;
        }

        public Command getCommand() {
            return command;
        }

        public SocketChannel getChannel() {
            return channel;
        }
    }

    public Worker(int workerId, PersistenceManager persistenceManager) {
        super("ConC-Worker-" + workerId);
        this.workerId = workerId;
        this.persistenceManager = persistenceManager;
    }

    public void submit(Command command, SocketChannel channel) {
        taskQueue.offer(new WorkerTask(command, channel));
    }

    @Override
    public void run() {
        System.out.println(getName() + " started.");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                
                cleanExpiredKeys();

                WorkerTask task = taskQueue.take();
                processTask(task);
            }
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrupted, shutting down.");
            Thread.currentThread().interrupt();
        }
    }

    private void processTask(WorkerTask task) {
        Command command = task.getCommand();
        SocketChannel channel = task.getChannel();

        String response;
        try {
            response = switch (command.getType()) {
                case SET -> handleSet(command);
                case GET -> handleGet(command);
                case DELETE -> handleDelete(command);
                case EXPIRE -> handleExpire(command);
                case TTL -> handleTtl(command);
                case EXISTS -> handleExists(command);
                case UNKNOWN -> "ERR unknown command\n";
            };
        } catch (Exception e) {
            response = "ERR execution failed: " + e.getMessage() + "\n";
        }

        sendResponse(channel, response);
    }

    private String handleSet(Command command) {
        String[] args = command.getArgs();
        if (args.length < 2) {
            return "ERR usage: SET key value [EX seconds]\n";
        }

        String key = args[0];
        String value = args[1];
        long expireAt = 0;

        if (args.length >= 4 && "EX".equalsIgnoreCase(args[2])) {
            try {
                long seconds = Long.parseLong(args[3]);
                expireAt = System.currentTimeMillis() + (seconds * 1000);
            } catch (NumberFormatException e) {
                return "ERR invalid expire seconds\n";
            }
        }

        CacheEntry entry = new CacheEntry(key, value, expireAt);
        store.set(entry);

        if (expireAt > 0) {
            ttlManager.add(entry);
        }

        persistenceManager.logWrite(command);

        return "OK\n";
    }

    private String handleGet(Command command) {
        String[] args = command.getArgs();
        if (args.length < 1) {
            return "ERR usage: GET key\n";
        }

        String key = args[0];
        Optional<CacheEntry> opt = store.get(key);

        if (opt.isEmpty()) {
            return "(nil)\n";
        }

        CacheEntry entry = opt.get();
        if (entry.isExpired()) {
            store.delete(key);
            return "(nil)\n";
        }

        return entry.getValue() + "\n";
    }

    private String handleDelete(Command command) {
        String[] args = command.getArgs();
        if (args.length < 1) {
            return "ERR usage: DELETE key\n";
        }

        String key = args[0];
        CacheEntry removed = store.delete(key);

        if (removed != null) {
            persistenceManager.logWrite(command);
            return "1\n";
        }
        return "0\n";
    }

    private String handleExpire(Command command) {
        String[] args = command.getArgs();
        if (args.length < 2) {
            return "ERR usage: EXPIRE key seconds\n";
        }

        String key = args[0];
        long seconds;
        try {
            seconds = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            return "ERR invalid seconds\n";
        }

        Optional<CacheEntry> opt = store.get(key);
        if (opt.isEmpty() || opt.get().isExpired()) {
            if (opt.isPresent()) store.delete(key); 
            return "0\n";
        }

        long expireAt = System.currentTimeMillis() + (seconds * 1000);
        CacheEntry updatedEntry = new CacheEntry(key, opt.get().getValue(), expireAt);
        store.set(updatedEntry);
        ttlManager.add(updatedEntry);

        persistenceManager.logWrite(command);
        return "1\n";
    }

    private String handleTtl(Command command) {
        String[] args = command.getArgs();
        if (args.length < 1) {
            return "ERR usage: TTL key\n";
        }

        String key = args[0];
        Optional<CacheEntry> opt = store.get(key);

        if (opt.isEmpty()) {
            return "-2\n";
        }

        CacheEntry entry = opt.get();
        if (entry.isExpired()) {
            store.delete(key);
            return "-2\n";
        }

        if (entry.getExpireAt() <= 0) {
            return "-1\n";
        }

        long diff = entry.getExpireAt() - System.currentTimeMillis();
        return Math.max(0, diff / 1000) + "\n";
    }

    private String handleExists(Command command) {
        String[] args = command.getArgs();
        if (args.length < 1) {
            return "ERR usage: EXISTS key\n";
        }

        String key = args[0];
        Optional<CacheEntry> opt = store.get(key);

        if (opt.isEmpty()) {
            return "0\n";
        }

        CacheEntry entry = opt.get();
        if (entry.isExpired()) {
            store.delete(key);
            return "0\n";
        }

        return "1\n";
    }

    private void cleanExpiredKeys() {
        long now = System.currentTimeMillis();
        while (true) {
            CacheEntry entry = ttlManager.peek();
            if (entry == null || entry.getExpireAt() > now) {
                break;
            }
            ttlManager.poll();
            Optional<CacheEntry> current = store.get(entry.getKey());
            if (current.isPresent() && current.get().getExpireAt() == entry.getExpireAt()) {
                store.delete(entry.getKey());
            }
        }
    }

    private void sendResponse(SocketChannel channel, String response) {
        if (channel == null || !channel.isOpen()) {
            return;
        }
        try {
            channel.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            System.err.println("Error writing to client socket: " + e.getMessage());
        }
    }

    public CacheStore getStore() {
        return store;
    }

    public TTLManager getTtlManager() {
        return ttlManager;
    }

    public int getTaskQueueSize() {
        return taskQueue.size();
    }
}
