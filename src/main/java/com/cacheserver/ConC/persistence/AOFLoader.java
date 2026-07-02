package com.cacheserver.ConC.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.cacheserver.ConC.command.Command;
import com.cacheserver.ConC.parser.PlainTextParser;
import com.cacheserver.ConC.router.RequestRouter;
import com.cacheserver.ConC.worker.WorkerPool;

public class AOFLoader {
    private final String aofPath;
    private final PlainTextParser parser = new PlainTextParser();

    public AOFLoader(String aofPath) {
        this.aofPath = aofPath;
    }

    public void load(RequestRouter router, WorkerPool workerPool) throws IOException {
        File file = new File(aofPath);
        if (!file.exists()) {
            System.out.println("AOF file not found at " + aofPath + ". Starting with empty database.");
            return;
        }

        System.out.println("Loading cache state from AOF file: " + aofPath);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int replayedCount = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                Command command = parser.parse(line);
                int workerIndex = router.getWorkerIndex(command);
                if (workerIndex != -1) {
                    
                    workerPool.getWorker(workerIndex).submit(command, null);
                    replayedCount++;
                }
            }
            System.out.println("Replayed " + replayedCount + " write commands from AOF.");

            waitForWorkers(workerPool);
        }
    }

    private void waitForWorkers(WorkerPool pool) {
        System.out.println("Waiting for workers to finish database restoration...");
        while (true) {
            boolean allEmpty = true;
            for (int i = 0; i < pool.getWorkerCount(); i++) {
                if (pool.getWorker(i).getTaskQueueSize() > 0) {
                    allEmpty = false;
                    break;
                }
            }
            if (allEmpty) {
                break;
            }
            try {
                Thread.sleep(10); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Database restoration complete.");
    }
}
