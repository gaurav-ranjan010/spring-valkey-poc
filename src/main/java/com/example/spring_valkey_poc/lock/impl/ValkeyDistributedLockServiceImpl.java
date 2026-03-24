package com.example.spring_valkey_poc.lock.impl;

import com.example.spring_valkey_poc.lock.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Valkey distributed lock using the classic SET NX PX pattern.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ValkeyDistributedLockServiceImpl implements DistributedLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Lua script for safe lock release.
     * KEYS[1] = lock key, ARGV[1] = expected lock value (UUID).
     * Returns 1 if deleted, 0 otherwise.
     */
    private static final String RELEASE_LOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else " +
                    "return 0 " +
                    "end";

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
            new DefaultRedisScript<>(RELEASE_LOCK_LUA, Long.class);

    private static final long RETRY_INTERVAL_MS = 50;

    @Override
    public String acquireLock(String lockKey, long waitTimeMs, long leaseTimeMs) {
        String lockValue = UUID.randomUUID().toString();
        long ttl = System.currentTimeMillis() + waitTimeMs;

        log.debug("Attempting to acquire lock '{}' (waitTime={}ms, lease={}ms)", lockKey, waitTimeMs, leaseTimeMs);

        while (System.currentTimeMillis() < ttl) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofMillis(leaseTimeMs));

            if (Boolean.TRUE.equals(acquired)) {
                log.info("Lock '{}' acquired successfully (value={})", lockKey, lockValue);
                return lockValue;
            }

            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Lock acquisition interrupted for key '{}'", lockKey);
                return null;
            }
        }

        log.warn("Failed to acquire lock '{}' within {}ms", lockKey, waitTimeMs);
        return null;
    }

    @Override
    public boolean releaseLock(String lockKey, String lockValue) {
        if (lockKey == null || lockValue == null) {
            return false;
        }

        Long result = redisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                Collections.singletonList(lockKey),
                lockValue
        );

        boolean released = result != null && result == 1L;
        if (released) {
            log.info("Lock '{}' released successfully (value={})", lockKey, lockValue);
        } else {
            log.warn("Lock '{}' was NOT released — value mismatch or already expired", lockKey);
        }
        return released;
    }
}

