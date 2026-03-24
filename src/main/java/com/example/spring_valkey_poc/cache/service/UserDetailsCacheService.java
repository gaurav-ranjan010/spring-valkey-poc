package com.example.spring_valkey_poc.cache.service;

import com.example.spring_valkey_poc.cache.repository.UserDetailsCacheRepository;
import com.example.spring_valkey_poc.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Repository
@RequiredArgsConstructor
public class UserDetailsCacheService {

    private final UserDetailsCacheRepository userDetailsCacheRepository;

    public UserEntity save(UserEntity request) {
        return userDetailsCacheRepository.save(String.valueOf(request.getId()), request);
    }

    public Optional<UserEntity> findById(long id) {
        return userDetailsCacheRepository.findById(String.valueOf(id));
    }

    public List<UserEntity> findAll() {
        return userDetailsCacheRepository.findAll();
    }

    public List<UserEntity> findAllByCity(String city) {
        return userDetailsCacheRepository.findAllByCity(city);
    }

    public void deleteById(String name) {
        userDetailsCacheRepository.deleteById(name);
    }

    public boolean exists(String name) {
        return userDetailsCacheRepository.existsById(name);
    }

    public long count() {
        return userDetailsCacheRepository.count();
    }
}
