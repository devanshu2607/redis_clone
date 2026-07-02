
# ConC (v2) - Sharded In-Memory Cache Server
ConC is a production-quality, lightweight in-memory cache engine inspired by Redis, built with a modern multi-threaded sharded architecture in Java.
It runs as a standalone server, utilizes deterministic key routing to eliminate global write locks, manages TTL expirations per shard, and logs state mutations asynchronously to an Append-Only File (AOF) for crash recovery.
---
## Architecture Overview
```text
                     Client
                        │
                  (TCP Socket)
                        │
                 NIO Selector Loop
                        │
                Plain Text Parser
                        │
                  Command Object
                        │
                 Request Router
             [hash(key) % THREAD_COUNT]
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
      Worker 1      Worker 2      Worker 3
      (Thread 1)    (Thread 2)    (Thread 3)
          │             │             │
       Shard 1       Shard 2       Shard 3
     (Map + TTL)   (Map + TTL)   (Map + TTL)
          │             │             │
          └─────────────┼─────────────┘
                        ▼
               Persistence Queue
                        │
             Persistence Logger Thread
                        │
                    cache.aof
Shared-Nothing Sharding: The database memory is split across independent worker threads. Each worker thread exclusively owns its own ConcurrentHashMap and TTLManager. There are no global locks, yielding near-infinite write throughput.
Deterministic Key Routing: Every command targeting the same key (SET, GET, DELETE, EXPIRE, TTL, EXISTS) is routed to the exact same worker thread using Math.abs(key.hashCode()) % THREAD_COUNT.
Non-Blocking Java NIO: Sockets are multiplexed using a Java NIO Selector for ultra-low connection latency.
Asynchronous AOF Persistence: Write commands are buffered in a queue and flushed to disk asynchronously by a dedicated thread to avoid blocking client requests.
Installation
You can install ConC globally on your machine directly from NPM:

bash


npm install -g cache-conc
(Note: ConC requires Java JRE/JDK 21 or higher installed on your system).

Getting Started
1. Start the Cache Server
Open a terminal window and launch the server globally:

bash


cache-conc
Expected Output:
text


Initializing ConC v2 Sharded Cache Server...
ConC-Worker-0 started.
ConC-Worker-1 started.
ConC-Worker-2 started.
ConC-AOF-Writer started.
NIO TCP Server listening on port 6379
2. Launch the Interactive CLI Client
Open a second terminal window and connect to the server globally (no files or folders needed!):

bash


cache-conc-cli
Expected Output:
text


Connected to ConC Cache Server!
Type your commands (e.g. SET key value, GET key). Type EXIT or QUIT to close.
ConC-CLI>
Supported Commands & Examples
1. SET
Stores a key-value pair in the cache. Supports optional TTL in seconds via EX.

text


ConC-CLI> SET session abc
OK
ConC-CLI> SET temp_key val EX 30
OK
2. GET
Retrieves the value of a key. Returns (nil) if the key does not exist or has expired.

text


ConC-CLI> GET session
abc
3. EXISTS
Checks if a key exists in the cache. Returns 1 if it exists, 0 otherwise.

text


ConC-CLI> EXISTS session
1
4. EXPIRE
Sets a time-to-live timeout (in seconds) on an existing key. Returns 1 if successful, 0 if the key does not exist.

text


ConC-CLI> EXPIRE session 10
1
5. TTL
Returns the remaining time-to-live (in seconds) of a key.

Returns the remaining seconds if it has a TTL.
Returns -1 if the key exists but has no TTL.
Returns -2 if the key does not exist.
text


ConC-CLI> TTL session
8
6. DELETE
Removes a key from the cache. Returns 1 if deleted, 0 if the key did not exist.

text


ConC-CLI> DELETE session
1
Crash Recovery (AOF)
Every write command (SET, DELETE, EXPIRE) is asynchronously appended to a cache.aof file in the directory where the server is run.

On server startup, the AOFLoader automatically reads cache.aof, parses the logs, and replays the commands through the router to restore the sharded cache tables to their exact state before shutdown.








