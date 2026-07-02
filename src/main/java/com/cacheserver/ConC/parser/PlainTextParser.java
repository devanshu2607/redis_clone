package com.cacheserver.ConC.parser;

import com.cacheserver.ConC.command.Command;
import com.cacheserver.ConC.command.CommandType;

public class PlainTextParser {

    public Command parse(String line) {
        if (line == null || line.isBlank()) {
            return new Command(CommandType.UNKNOWN, new String[0]);
        }

        String[] parts = line.trim().split("\\s+");
        String commandName = parts[0].toUpperCase();

        CommandType type;
        
        if ("DEL".equals(commandName)) {
            commandName = "DELETE";
        }

        try {
            type = CommandType.valueOf(commandName);
        } catch (IllegalArgumentException e) {
            type = CommandType.UNKNOWN;
        }

        String[] args = new String[Math.max(0, parts.length - 1)];
        if (parts.length > 1) {
            System.arraycopy(parts, 1, args, 0, parts.length - 1);
        }

        System.out.println("PARSER " + commandName + " -> " + type);

        return new Command(type, args);
    }
}
