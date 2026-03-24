package com.example.spring_valkey_poc.cache.repository.impl;

import com.example.spring_valkey_poc.cache.repository.AbstractCacheRepository;
import com.example.spring_valkey_poc.cache.repository.UserDetailsCacheRepository;
import com.example.spring_valkey_poc.entity.UserEntity;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
public class UserDetailsCacheRepositoryImpl
        extends AbstractCacheRepository<UserEntity, String>
        implements UserDetailsCacheRepository {

    public UserDetailsCacheRepositoryImpl(RedisTemplate<String, Object> redisTemplate) {
        super(redisTemplate, "userDetails", Duration.ofMinutes(5));
    }

    @Override
    public List<UserEntity> findAllByCity(String city) {
        return findAll().stream()
                        .filter(user -> city.equalsIgnoreCase(user.getCity()))
                        .toList();
    }
}
