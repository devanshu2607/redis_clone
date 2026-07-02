package com.cacheserver.ConC.command;

public class Command {
    private final CommandType type;
    private final String[] args;

    public Command(CommandType type, String[] args) {
        this.type = type;
        this.args = args != null ? args : new String[0];
    }

    public CommandType getType() {
        return type;
    }

    public String[] getArgs() {
        return args;
    }

    public String getKey() {
        return args.length > 0 ? args[0] : null;
    }
}
