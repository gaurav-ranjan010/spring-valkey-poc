package com.example.spring_valkey_poc.cache.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.*;

public abstract class AbstractCacheRepository<T, ID> implements CacheRepository<T, ID> {

    private static final Logger log = LoggerFactory.getLogger(AbstractCacheRepository.class);

    protected final RedisTemplate<String, Object> redisTemplate;
    protected final String cachePrefix;
    protected final Duration ttl;

    protected AbstractCacheRepository(RedisTemplate<String, Object> redisTemplate,
            String cachePrefix,
            Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.cachePrefix = cachePrefix;
        this.ttl = ttl;
    }

    protected String buildKey(ID id) {
        return cachePrefix + "::" + id;
    }

    @Override
    public T save(ID id, T entity) {
        try {
            redisTemplate.opsForValue().set(buildKey(id), entity, ttl);
        } catch (Exception e) {
            log.warn("Failed to write cache key '{}': {}", buildKey(id), e.getMessage());
        }
        return entity;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> findById(ID id) {
        try {
            Object value = redisTemplate.opsForValue().get(buildKey(id));
            return Optional.ofNullable((T) value);
        } catch (Exception e) {
            log.warn("Stale or corrupt cache entry for key '{}'. Evicting and falling back to DB. Reason: {}",
                    buildKey(id), e.getMessage());
            evict(id);
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> findAll() {
        try {
            Set<String> keys = redisTemplate.keys(cachePrefix + "::*");
            if (keys == null || keys.isEmpty()) return Collections.emptyList();

            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            if (values == null) return Collections.emptyList();

            List<T> result = new ArrayList<>();
            Iterator<String> keyIterator = keys.iterator();
            for (Object value : values) {
                String key = keyIterator.hasNext() ? keyIterator.next() : null;
                if (value == null) continue;
                try {
                    result.add((T) value);
                } catch (Exception e) {
                    log.warn("Stale or corrupt cache entry for key '{}'. Evicting. Reason: {}", key, e.getMessage());
                    if (key != null) redisTemplate.delete(key);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to fetch all cache entries for prefix '{}': {}", cachePrefix, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public boolean existsById(ID id) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(id)));
        } catch (Exception e) {
            log.warn("Failed to check existence of cache key '{}': {}", buildKey(id), e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteById(ID id) {
        try {
            redisTemplate.delete(buildKey(id));
        } catch (Exception e) {
            log.warn("Failed to delete cache key '{}': {}", buildKey(id), e.getMessage());
        }
    }

    /**
     * Internal eviction helper — removes a single key by ID without going through
     * the public deleteById contract (which subclasses may override).
     */
    private void evict(ID id) {
        try {
            redisTemplate.delete(buildKey(id));
        } catch (Exception ex) {
            log.error("Failed to evict cache key '{}': {}", buildKey(id), ex.getMessage());
        }
    }

    @Override
    public void deleteAll() {
        try {
            Set<String> keys = redisTemplate.keys(cachePrefix + "::*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to delete all cache entries for prefix '{}': {}", cachePrefix, e.getMessage());
        }
    }

    @Override
    public long count() {
        try {
            Set<String> keys = redisTemplate.keys(cachePrefix + "::*");
            return keys == null ? 0 : keys.size();
        } catch (Exception e) {
            log.warn("Failed to count cache entries for prefix '{}': {}", cachePrefix, e.getMessage());
            return 0;
        }
    }
}
