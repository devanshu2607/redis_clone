package com.cacheserver.ConC.core.util;

import java.nio.charset.StandardCharsets;

public final class MemoryEstimator {

    private static final long ENTRY_OVERHEAD_BYTES = 64L;

    private MemoryEstimator() {
    }

    public static long estimateEntrySize(String key, String value) {
        long keySize = key.getBytes(StandardCharsets.UTF_8).length;
        long valueSize = value.getBytes(StandardCharsets.UTF_8).length;
        return ENTRY_OVERHEAD_BYTES + keySize + valueSize;
    }
}