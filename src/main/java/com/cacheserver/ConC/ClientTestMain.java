package com.cacheserver.ConC;

import java.util.Optional;

import com.cacheserver.ConC.client.TcpCacheClient;
import com.cacheserver.ConC.client.config.CacheClientConfig;

public class ClientTestMain {

    public static void main(String[] args) {
        CacheClientConfig config = new CacheClientConfig("localhost", 6379, 60);

        try (TcpCacheClient client = new TcpCacheClient(config)) {
            client.set("name", "devanshu");
            Optional<String> value = client.get("name");
            System.out.println("GET name -> " + value.orElse("missing"));

            System.out.println("EXISTS name -> " + client.exists("name"));
            System.out.println("TTL name -> " + client.ttl("name"));

            boolean deleted = client.delete("name");
            System.out.println("DELETE name -> " + deleted);

            System.out.println("GET after delete -> " + client.get("name").orElse("missing"));
        }
    }
}