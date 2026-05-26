package com.cacheserver.ConC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class TestTcpClient {

    public static void main(String[] args) throws Exception {
        try (
                Socket socket = new Socket("localhost", 6379);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))
        ) {
            System.out.println("Server: " + reader.readLine());

            sendCommand("SET k1 1234567890", reader, writer);
            sendCommand("SET k2 abcdefghij", reader, writer);
            sendCommand("SET k3 qwertyuiop", reader, writer);

            sendCommand("EXISTS k1", reader, writer);
            sendCommand("EXISTS k2", reader, writer);
            sendCommand("EXISTS k3", reader, writer);

            sendCommand("GET k2", reader, writer);

            sendCommand("SET k4 zzzzzzzzzz", reader, writer);

            sendCommand("EXISTS k1", reader, writer);
            sendCommand("EXISTS k2", reader, writer);
            sendCommand("EXISTS k3", reader, writer);
            sendCommand("EXISTS k4", reader, writer);
        }
    }

    private static void sendCommand(String command, BufferedReader reader, BufferedWriter writer) throws Exception {
        writer.write(command);
        writer.newLine();
        writer.flush();

        String response = reader.readLine();
        System.out.println("Command: " + command);
        System.out.println("Response: " + response);
        System.out.println();
    }
}