package com.cacheserver.ConC.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

import com.cacheserver.ConC.command.Command;
import com.cacheserver.ConC.parser.PlainTextParser;
import com.cacheserver.ConC.router.RequestRouter;
import com.cacheserver.ConC.worker.WorkerPool;

public class ClientHandler {
    private final SocketChannel channel;
    private final StringBuilder buffer = new StringBuilder();
    private final PlainTextParser parser;
    private final RequestRouter router;
    private final WorkerPool workerPool;

    public ClientHandler(SocketChannel channel, PlainTextParser parser, RequestRouter router, WorkerPool workerPool) {
        this.channel = channel;
        this.parser = parser;
        this.router = router;
        this.workerPool = workerPool;
    }

    public void handleRead() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        int read = channel.read(byteBuffer);
        if (read == -1) {
            close();
            return;
        }

        byteBuffer.flip();
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        buffer.append(new String(bytes, StandardCharsets.UTF_8));

        int newlineIndex;
        while ((newlineIndex = buffer.indexOf("\n")) != -1) {
            String line = buffer.substring(0, newlineIndex).trim();
            buffer.delete(0, newlineIndex + 1);

            if (!line.isEmpty()) {
                Command command = parser.parse(line);
                int workerIndex = router.getWorkerIndex(command);
                if (workerIndex != -1) {
                    
                    workerPool.getWorker(workerIndex).submit(command, channel);
                } else {
                    channel.write(ByteBuffer.wrap("ERR invalid command\n".getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    public void close() {
        try {
            if (channel.isOpen()) {
                channel.close();
            }
        } catch (IOException ignored) {
        }
    }
}
