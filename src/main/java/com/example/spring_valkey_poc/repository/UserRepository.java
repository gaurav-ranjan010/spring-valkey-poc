package com.example.spring_valkey_poc.repository;

import com.example.spring_valkey_poc.entity.UserEntity;
import com.example.spring_valkey_poc.records.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity , Long> {

    List<UserRecord> findByName(String name);
}
