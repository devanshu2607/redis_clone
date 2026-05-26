package com.cacheserver.ConC.tcp.server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.cacheserver.ConC.core.engine.CacheEngine;

public class TcpCacheServer {

    private final int port;
    private final CacheEngine cacheEngine;
    private final ExecutorService clientPool;

    public TcpCacheServer(int port, CacheEngine cacheEngine) {
        this.port = port;
        this.cacheEngine = cacheEngine;
        this.clientPool = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientPool.submit(new ClientHandler(clientSocket, cacheEngine));
            }
        }
    }
}
