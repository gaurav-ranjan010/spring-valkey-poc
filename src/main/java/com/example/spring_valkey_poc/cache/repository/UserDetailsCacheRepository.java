package com.example.spring_valkey_poc.cache.repository;

import com.example.spring_valkey_poc.entity.UserEntity;

import java.util.List;


public interface UserDetailsCacheRepository extends CacheRepository<UserEntity, String> {

    List<UserEntity> findAllByCity(String city);
}
