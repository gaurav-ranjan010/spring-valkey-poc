package com.example.spring_valkey_poc.lock;

public interface DistributedLockService {

    /**
     * Try to acquire the lock identified by {@code lockKey}.
     *
     * @param lockKey     unique key for the lock
     * @return an opaque lock value needed for {@link #releaseLock}, or {@code null} if acquisition failed
     */
    boolean acquireLock(String lockKey,  String lockValue);

    /**
     * Release a previously acquired lock.
     * No-op if lockKey or lockValue is null.
     *
     * @param lockKey   the key used during acquisition
     * @param lockValue the value returned by {@link #acquireLock}
     */
    void releaseLock(String lockKey, String lockValue);
}
