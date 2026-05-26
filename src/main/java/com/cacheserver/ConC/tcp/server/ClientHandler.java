package com.cacheserver.ConC.tcp.server;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import com.cacheserver.ConC.core.engine.CacheEngine;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final CacheEngine cacheEngine;

    public ClientHandler(Socket socket, CacheEngine cacheEngine) {
        this.socket = socket;
        this.cacheEngine = cacheEngine;
    }

    @Override
    public void run() {
        try (
                socket;
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            writer.write("CONNECTED\n");
            writer.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                String response = cacheEngine.execute(line);
                writer.write(response);
                writer.write("\n");
                writer.flush();
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + e.getMessage());
        }
    }
}