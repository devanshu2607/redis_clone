package com.cacheserver.ConC.router;

import com.cacheserver.ConC.command.Command;

public class RequestRouter {
    private final int workerCount;

    public RequestRouter(int workerCount) {
        this.workerCount = workerCount;
    }

    public int getWorkerIndex(Command command) {
        String key = command.getKey();
        if (key == null) {
            return -1;
        }
        return Math.abs(key.hashCode()) % workerCount;
    }
}
