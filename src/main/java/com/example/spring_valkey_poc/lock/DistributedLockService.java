package com.example.spring_valkey_poc.lock;

public interface DistributedLockService {

    boolean acquireLock(String lockKey,  String lockValue);

    void releaseLock(String lockKey, String lockValue);
}
