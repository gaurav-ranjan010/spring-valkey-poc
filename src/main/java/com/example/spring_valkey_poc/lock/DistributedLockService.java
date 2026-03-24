package com.example.spring_valkey_poc.lock;

public interface DistributedLockService {

    /**
     * Try to acquire the lock identified by {@code lockKey}.
     *
     * @param lockKey     unique key for the lock
     * @param waitTimeMs  maximum time (ms) to keep retrying
     * @param leaseTimeMs auto-release time (ms) after acquisition
     * @return an opaque lock value needed for {@link #releaseLock}, or {@code null} if acquisition failed
     */
    String acquireLock(String lockKey, long waitTimeMs, long leaseTimeMs);

    boolean releaseLock(String lockKey, String lockValue);
}

