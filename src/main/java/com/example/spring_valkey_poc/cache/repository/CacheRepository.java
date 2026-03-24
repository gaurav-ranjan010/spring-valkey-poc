package com.example.spring_valkey_poc.cache.repository;

import java.util.List;
import java.util.Optional;

public interface CacheRepository <T , ID> {
    T save(ID id, T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    boolean existsById(ID id);

    void deleteById(ID id);

    void deleteAll();

    long count();
}
