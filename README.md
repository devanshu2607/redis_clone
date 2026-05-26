# ConC

ConC is a lightweight Redis-inspired local cache server built in Java.

It runs as a standalone TCP server on a user's machine, uses the user's local RAM, supports TTL expiration, LRU eviction, key limits, memory limits, and provides a Java client for backend integration.

The goal of this project is to learn and build backend infrastructure concepts such as TCP networking, concurrency, memory management, cache eviction, and client-server architecture.

---

## What This Project Does

ConC behaves like a lightweight local cache server that:

- runs as a standalone Java process
- listens on a TCP port
- stores data in memory using the user's machine RAM
- supports Redis-style text commands
- supports TTL expiration
- supports LRU eviction
- enforces max key and memory limits
- can be run locally using JAR or Docker
- can be accessed from backend applications using a Java client

---

## Features

- Standalone local TCP cache server
- In-memory storage using `ConcurrentHashMap`
- Redis-style plain text command protocol
- TTL support using expiry timestamps
- Background expiry cleaner
- LRU eviction
- Max key limit
- Approximate max memory limit
- Runnable JAR packaging
- Docker support
- Java client package
- `getOrLoad(...)` helper for cache-aside style usage

---

## Architecture Overview

```text
Backend App / Client
        |
        v
   TcpCacheClient
        |
        v
   TCP Socket Connection
        |
        v
   TcpCacheServer
        |
        v
   ClientHandler
        |
        v
   CommandParser
        |
        v
   CacheEngine
        |
        +--> InMemoryCacheStore
        |       |
        |       v
        |   ConcurrentHashMap<String, CacheEntry>
        |
        +--> ExpiryManager
        |       |
        |       v
        |   PriorityQueue<ExpiryNode>
        |
        +--> LruEvictionManager
