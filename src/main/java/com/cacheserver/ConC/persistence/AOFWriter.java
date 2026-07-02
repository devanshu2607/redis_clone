package com.cacheserver.ConC.persistence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.cacheserver.ConC.command.Command;

public class AOFWriter extends Thread {
    private final BlockingQueue<Command> queue;
    private final String aofPath;
    private volatile boolean running = true;

    public AOFWriter(BlockingQueue<Command> queue, String aofPath) {
        super("ConC-AOF-Writer");
        this.queue = queue;
        this.aofPath = aofPath;
    }

    @Override
    public void run() {
        System.out.println(getName() + " started.");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(aofPath, true))) {
            while (running || !queue.isEmpty()) {
                
                Command command = queue.poll(100, TimeUnit.MILLISECONDS);
                if (command != null) {
                    writeCommand(writer, command);
                }
            }
        } catch (IOException e) {
            System.err.println("AOFWriter encountered IO error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println(getName() + " interrupted, shutting down.");
            Thread.currentThread().interrupt();
        }
    }

    private void writeCommand(BufferedWriter writer, Command command) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(command.getType().name());
        for (String arg : command.getArgs()) {
            sb.append(" ").append(arg);
        }
        sb.append("\n");
        writer.write(sb.toString());
        writer.flush(); 
    }

    public void shutdown() {
        running = false;
        this.interrupt();
    }
}
