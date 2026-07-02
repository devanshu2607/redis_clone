package com.cacheserver.ConC;

import java.io.IOException;
import com.cacheserver.ConC.network.SocketServer;
import com.cacheserver.ConC.parser.PlainTextParser;
import com.cacheserver.ConC.persistence.AOFLoader;
import com.cacheserver.ConC.persistence.PersistenceManager;
import com.cacheserver.ConC.router.RequestRouter;
import com.cacheserver.ConC.worker.WorkerPool;

public class Main {
    private static final int PORT = 6379;
    private static final int THREAD_COUNT = 3;
    private static final String AOF_PATH = "cache.aof";

    public static void main(String[] args) {
        System.out.println("Initializing ConC v2 Sharded Cache Server...");

        PersistenceManager persistenceManager = new PersistenceManager(AOF_PATH);
        RequestRouter router = new RequestRouter(THREAD_COUNT);
        WorkerPool workerPool = new WorkerPool(THREAD_COUNT, persistenceManager);

        persistenceManager.start();
        workerPool.start();

        try {
            AOFLoader loader = new AOFLoader(AOF_PATH);
            loader.load(router, workerPool);
        } catch (IOException e) {
            System.err.println("Fatal Error: Failed to restore state from AOF file: " + e.getMessage());
            System.exit(1);
        }

        PlainTextParser parser = new PlainTextParser();
        SocketServer socketServer = new SocketServer(PORT, parser, router, workerPool);
        socketServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received. Terminating ConC...");
            socketServer.shutdown();
            workerPool.shutdown();
            persistenceManager.shutdown();
            System.out.println("ConC server terminated gracefully.");
        }, "ConC-Shutdown-Hook"));
    }
}