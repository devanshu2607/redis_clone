package com.cacheserver.ConC.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import com.cacheserver.ConC.parser.PlainTextParser;
import com.cacheserver.ConC.router.RequestRouter;
import com.cacheserver.ConC.worker.WorkerPool;

public class SocketServer extends Thread {
    private final int port;
    private final PlainTextParser parser;
    private final RequestRouter router;
    private final WorkerPool workerPool;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = true;

    public SocketServer(int port, PlainTextParser parser, RequestRouter router, WorkerPool workerPool) {
        super("ConC-NIO-SocketServer");
        this.port = port;
        this.parser = parser;
        this.router = router;
        this.workerPool = workerPool;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("NIO TCP Server listening on port " + port);

            while (running && !Thread.currentThread().isInterrupted()) {
                if (selector.select() == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("NIO Server encountered exception: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);

        ClientHandler handler = new ClientHandler(client, parser, router, workerPool);
        client.register(selector, SelectionKey.OP_READ, handler);

        client.write(ByteBuffer.wrap("CONNECTED\n".getBytes(StandardCharsets.UTF_8)));
    }

    private void handleRead(SelectionKey key) {
        ClientHandler handler = (ClientHandler) key.attachment();
        try {
            handler.handleRead();
        } catch (IOException e) {
            
            handler.close();
            key.cancel();
        }
    }

    public void shutdown() {
        running = false;
        if (selector != null) {
            selector.wakeup();
        }
        this.interrupt();
    }

    private void cleanup() {
        System.out.println("Cleaning up SocketServer resources...");
        try {
            if (serverChannel != null) {
                serverChannel.close();
            }
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Error during server cleanup: " + e.getMessage());
        }
    }
}
