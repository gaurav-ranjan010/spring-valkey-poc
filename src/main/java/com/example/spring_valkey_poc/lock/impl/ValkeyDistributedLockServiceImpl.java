package com.example.spring_valkey_poc.lock.impl;

import com.example.spring_valkey_poc.lock.DistributedLockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

/**
 * Valkey distributed lock using the classic SET NX PX pattern.
 * <p>
 * <b>Acquire:</b> {@code SET lockKey uuid NX PX leaseTimeMs} — sets the key only
 * if it does not already exist, with an automatic expiry to prevent deadlocks.
 * <p>
 * <b>Release:</b> Atomic Lua script that deletes the key only when its value
 * matches the UUID of the holder, preventing accidental release by another thread.
 */
@Service
@Slf4j
public class ValkeyDistributedLockServiceImpl implements DistributedLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    private final long leaseTimeMs;

    private static final String RELEASE_LOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else " +
                    "return 0 " +
                    "end";

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT =
            new DefaultRedisScript<>(RELEASE_LOCK_LUA, Long.class);

    public ValkeyDistributedLockServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            @Value("${valkey.lock.lease-time-ms:5000}") long leaseTimeMs) {
        this.redisTemplate = redisTemplate;
        this.leaseTimeMs = leaseTimeMs;
    }

    @Override
    public boolean acquireLock(String lockKey, String lockValue) {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofMillis(leaseTimeMs));

            if (Boolean.TRUE.equals(acquired)) {
                log.info("Lock '{}' acquired", lockKey);
                return true;
            }
            log.warn("Failed to acquire lock '{}'", lockKey);
            return false;
        } catch (Exception e) {
            log.error("Error while acquiring lock '{}': {}", lockKey, e.getMessage());
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey, String lockValue) {
        if (lockKey == null || lockValue == null) {
            return;
        }

        try {
            Long result = redisTemplate.execute(
                    RELEASE_LOCK_SCRIPT,
                    Collections.singletonList(lockKey),
                    lockValue
            );

            if (result != null && result == 1L) {
                log.info("Lock '{}' released", lockKey);
            } else {
                log.warn("Lock '{}' already expired before release", lockKey);
            }
        } catch (Exception e) {
            log.error("Error while releasing lock '{}': {}. Lock will auto-expire after {}ms",
                    lockKey, e.getMessage(), leaseTimeMs);
        }
    }
}

