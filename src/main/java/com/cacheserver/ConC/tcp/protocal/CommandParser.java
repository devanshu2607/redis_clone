package com.cacheserver.ConC.tcp.protocal;

public class CommandParser {

    public Command parse(String line) {
        if (line == null || line.isBlank()) {
            return new Command(CommandType.UNKNOWN, new String[0]);
        }

        String[] parts = line.trim().split("\\s+");
        String commandName = parts[0].toUpperCase();

        CommandType type = switch (commandName) {
            case "PING" -> CommandType.PING;
            case "SET" -> CommandType.SET;
            case "GET" -> CommandType.GET;
            case "DEL" -> CommandType.DEL;
            case "EXISTS" -> CommandType.EXISTS;
            case "TTL" -> CommandType.TTL;
            default -> CommandType.UNKNOWN;
        };

        String[] args = new String[Math.max(0, parts.length - 1)];
        if (parts.length > 1) {
            System.arraycopy(parts, 1, args, 0, parts.length - 1);
        }

        return new Command(type, args);
    }
}