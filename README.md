# ConC

ConC is a lightweight Redis-inspired local cache server built in Java.

It runs as a standalone TCP server on a user's machine, uses the user's local RAM, supports TTL expiration, LRU eviction, key limits, memory limits, and provides a Java client for backend integration.

The goal of this project is to learn and build backend infrastructure concepts such as TCP networking, concurrency, memory management, cache eviction, and client-server architecture.

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
Core Concepts Used
This project uses:

ServerSocket and Socket
ConcurrentHashMap
PriorityQueue
ScheduledExecutorService
background cleaner thread
LRU eviction by access tracking
memory estimation
Java client/server communication
Project Structure
src/main/java/com/cacheserver/ConC
|
+- Main.java
+- TestTcpClient.java
+- ClientTestMain.java
|
+- client
|  +- CacheClient.java
|  +- TcpCacheClient.java
|  +- CacheClientException.java
|  +- config
|     +- CacheClientConfig.java
|
+- core
|  +- config
|  |  +- CacheConfig.java
|  |
|  +- engine
|  |  +- CacheEngine.java
|  |  +- CacheEngineImpl.java
|  |
|  +- eviction
|  |  +- LruEvictionManager.java
|  |  +- EvictionPolicy.java
|  |  +- MemoryLimitManager.java
|  |
|  +- expiry
|  |  +- ExpiryNode.java
|  |  +- ExpiryManager.java
|  |  +- ExpiryCleaner.java
|  |
|  +- model
|  |  +- CacheEntry.java
|  |  +- CacheStats.java
|  |
|  +- store
|  |  +- CacheStore.java
|  |  +- InMemoryCacheStore.java
|  |
|  +- util
|     +- MemoryEstimator.java
|     +- TimeUtil.java
|
+- tcp
   +- protocal
   |  +- Command.java
   |  +- CommandParser.java
   |  +- CommandType.java
   |
   +- server
      +- TcpCacheServer.java
      +- ClientHandler.java
Note:

protocal should ideally be renamed to protocol later.
old Spring REST code can be ignored if TCP-only mode is the final path.
Supported Commands
ConC currently supports these commands:

PING
PING
Response:

PONG
SET key value
SET user devanshu
Response:

OK
SET key value EX seconds
SET session abc123 EX 60
Response:

OK
Meaning:

store key with TTL
key expires after the given number of seconds
GET key
GET user
Response:

devanshu
If key is missing:

(nil)
DEL key
DEL user
Response:

1 if key deleted
0 if key not found
EXISTS key
EXISTS user
Response:

1 if key exists
0 if key does not exist
TTL key
TTL session
Response:

remaining seconds if TTL exists
-1 if key exists but has no TTL
-2 if key does not exist
Response Behavior Summary
PING -> PONG
SET -> OK
GET missing-key -> (nil)
DEL existing-key -> 1
DEL missing-key -> 0
EXISTS existing-key -> 1
EXISTS missing-key -> 0
TTL existing-key with TTL -> remaining seconds
TTL existing-key without TTL -> -1
TTL missing-key -> -2
Cache Entry Design
Each cached value is stored as a CacheEntry.

It contains:

value
createdAt
lastAccessTime
expiryTime
hasTTL
estimatedSizeBytes
This metadata allows:

TTL expiration
LRU eviction
memory tracking
TTL Expiration Design
TTL support is implemented using:

expiryTime in CacheEntry
PriorityQueue<ExpiryNode> for nearest expiry tracking
ScheduledExecutorService cleaner thread
How it works:

TTL key is inserted into map
same key expiry metadata goes into min-heap
cleaner thread periodically checks nearest expiry
expired keys are removed
lazy expiry also happens on GET, EXISTS, and TTL
This means TTL cleanup works in two ways:

active cleanup
lazy cleanup on access
LRU Eviction Design
LRU eviction is used when:

maxKeys is exceeded
maxMemoryBytes is exceeded
How it works:

every successful GET updates lastAccessTime
new writes may trigger eviction
least recently used key is removed first
TTL and LRU work independently:

TTL = time-based expiration
LRU = memory pressure based eviction
Memory Management
ConC uses the local machine memory of the user running the server.

Important points:

data lives inside JVM heap
JVM heap ultimately uses machine RAM
ConcurrentHashMap is not unlimited
without limits, memory pressure can grow
Current controls:

maxKeys
maxMemoryBytes
Memory estimation is approximate and based on:

key string bytes
value string bytes
a fixed entry overhead estimate
Run Locally
1. Build project
./mvnw clean package
2. Run the server JAR
java -jar target/ConC-0.0.1-SNAPSHOT.jar
Expected output:

TCP cache server started on port 6379
Run With Maven
Run server
./mvnw exec:java -Dexec.mainClass="com.cacheserver.ConC.Main"
Run Java client test
./mvnw exec:java -Dexec.mainClass="com.cacheserver.ConC.ClientTestMain"
Run With Docker
Build image
docker build --no-cache -t conc-cache .
Run container
docker run -p 6379:6379 conc-cache
If host port 6379 is already busy:

docker run -p 6380:6379 conc-cache
In that case the container still listens on 6379, but your machine will expose it through 6380.

Java Client Usage
ConC includes a Java client package for backend integration.

Basic string usage
CacheClientConfig config = new CacheClientConfig("localhost", 6379, 60);

try (TcpCacheClient client = new TcpCacheClient(config)) {
    client.set("name", "devanshu");
    System.out.println(client.get("name").orElse("missing"));
}
String operations
client.set("user:1", "devanshu");
client.set("session:1", "abc123", 120);

Optional<String> value = client.get("user:1");
boolean exists = client.exists("user:1");
long ttl = client.ttl("session:1");
boolean deleted = client.delete("user:1");
Cache-Aside Style Usage
This client also supports a cache-aside helper method:

User user = client.getOrLoad(
    "user:1",
    User.class,
    () -> userRepository.findById(1L).orElseThrow(),
    120
);
This is useful because:

database logic stays inside the user's backend
ConC only handles caching
every project can use its own repository/entity structure
This makes the package reusable across different backend projects.

Example Integration Flow
A backend app can use ConC like this:

Incoming request
   |
   v
Try cache using TcpCacheClient
   |
   +--> if hit, return cached value
   |
   +--> if miss, load from database
            |
            v
        store in cache
            |
            v
        return response
This matches the cache-aside pattern.

Current Config
Current config is set programmatically through CacheConfig inside Main.java.

Example values include:

port: 6379
cleaner interval: 1 second
max keys: configured in code
max memory bytes: configured in code
These can later be moved to:

properties file
environment variables
Docker env
CLI args
Testing Notes
Current test paths include:

TestTcpClient.java
ClientTestMain.java
They help verify:

socket communication
command handling
TTL behavior
LRU behavior
Java client integration
Current Limitations
Current version does not yet support:

persistence
snapshotting
append-only logs
authentication
metrics dashboard
replication
clustering
pub/sub
binary protocol
multi-word value parsing with spaces
non-Java client SDKs
Also:

memory estimation is approximate
LRU implementation is functional but not highly optimized
thread-per-client model is simple, not hyperscale optimized
Future Improvements
Possible next upgrades:

persistence to disk
snapshot restore on startup
append-only logging
Node client package
Python client package
npm wrapper / launcher
authentication
metrics and observability
protocol improvements
config via env or properties
AWS deployment docs
Jenkins pipeline
connection pooling in client
more efficient LRU data structure
Learning Goals Covered
This project teaches:

TCP socket programming
client-server communication
concurrency basics
in-memory caching
TTL expiration systems
heap-based scheduling
LRU eviction
memory-pressure handling
Docker packaging
reusable backend infrastructure design
Why This Project Is Useful
ConC is useful as:

a backend systems learning project
a Redis-inspired architecture exercise
a local installable cache server
a Java client + TCP server system
a practical infra-style project for portfolio and understanding
