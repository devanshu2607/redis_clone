package com.cacheserver.ConC;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.cacheserver.ConC.core.config.CacheConfig;
import com.cacheserver.ConC.core.engine.CacheEngine;
import com.cacheserver.ConC.core.engine.CacheEngineImpl;
import com.cacheserver.ConC.core.expiry.ExpiryCleaner;
import com.cacheserver.ConC.core.expiry.ExpiryManager;
import com.cacheserver.ConC.core.store.InMemoryCacheStore;
import com.cacheserver.ConC.tcp.server.TcpCacheServer;

public class Main {

    public static void main(String[] args) throws Exception {
        CacheConfig cacheConfig = new CacheConfig(
                6379,
                1000,
                1,
                220
        );

        InMemoryCacheStore cacheStore = new InMemoryCacheStore();
        ExpiryManager expiryManager = new ExpiryManager();

        CacheEngine cacheEngine = new CacheEngineImpl(
                cacheStore,
                cacheStore,
                expiryManager,
                cacheConfig
        );

        TcpCacheServer tcpCacheServer = new TcpCacheServer(cacheConfig.getPort(), cacheEngine);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                new ExpiryCleaner(cacheEngine, expiryManager),
                1,
                cacheConfig.getCleanerIntervalSeconds(),
                TimeUnit.SECONDS
        );

        System.out.println("TCP cache server started on port " + cacheConfig.getPort());
        tcpCacheServer.start();
    }
}