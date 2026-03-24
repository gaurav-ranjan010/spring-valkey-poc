package com.example.spring_valkey_poc.service.impl;

import com.example.spring_valkey_poc.cache.service.UserDetailsCacheService;
import com.example.spring_valkey_poc.entity.UserEntity;
import com.example.spring_valkey_poc.enums.ErrorCodes;
import com.example.spring_valkey_poc.exception.GlobalException;
import com.example.spring_valkey_poc.lock.DistributedLockService;
import com.example.spring_valkey_poc.nonentity.UserDetailsRequest;
import com.example.spring_valkey_poc.nonentity.UserDetailsResponse;
import com.example.spring_valkey_poc.repository.UserRepository;
import com.example.spring_valkey_poc.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserDetailsCacheService userDetailsCacheService;

    private final DistributedLockService distributedLockService;

    private static final String UPDATE_USER_LOCK_PREFIX = "lock:user:update:";

    @Override
    public UserDetailsResponse fetchUserById(long id) {
        log.info("Received request to fetch user details for id : {}", id);
        try {
            Optional<UserEntity> cached = userDetailsCacheService.findById(id);
            if (cached.isPresent()) {
                log.info("Cache hit for id : {}", id);
                return buildUserDetailsResponse(cached.get());
            }

            log.info("Cache miss for id : {}. Fetching from DB.", id);
            UserEntity userEntity = userRepository.findById(id)
                    .orElseThrow(() -> new GlobalException(ErrorCodes.USER_NOT_FOUND));

            userDetailsCacheService.save(userEntity);
            log.info("Saved id : {} in cache", id);
            return buildUserDetailsResponse(userEntity);
        } catch (Exception e) {
            log.error("Unable to fetch user details for id : {}. Exception : {}", id, e.getMessage());
            throw new GlobalException(ErrorCodes.USER_NOT_FOUND);
        }
    }

    @Override
    public UserDetailsResponse addUser(UserDetailsRequest userDetailsRequest) {
        log.info("Received request to add user with details : {}", userDetailsRequest);
        try {
            UserEntity userEntity = buildUserEntityFromRequest(userDetailsRequest);
            userEntity = userRepository.save(userEntity);
            log.info("User added successfully with id : {}", userEntity.getId());
            return buildUserDetailsResponse(userEntity);
        } catch (Exception exception) {
            log.error("Unable to add user : {}. Exception : {}", userDetailsRequest, exception.getMessage());
            throw new GlobalException(ErrorCodes.UNABLE_TO_ADD_USER);
        }
    }

    /**
     * Update user details using a distributed lock.
     */
    @Override
    public UserDetailsResponse updateUser(long id, UserDetailsRequest userDetailsRequest) {
        log.info("Received request to update user id : {} with details : {}", id, userDetailsRequest);

        String lockKey = UPDATE_USER_LOCK_PREFIX + id;
        String lockValue = UUID.randomUUID().toString();
        boolean isAcquired = distributedLockService.acquireLock(lockKey, lockValue);

        if (!isAcquired) {
            log.error("Failed to acquire lock for updating user id : {}", id);
            throw new GlobalException(ErrorCodes.LOCK_ACQUISITION_FAILED);
        }

        try {
            UserEntity userEntity = userRepository.findById(id)
                    .orElseThrow(() -> new GlobalException(ErrorCodes.USER_NOT_FOUND));
            updateUserEntity(userDetailsRequest, userEntity);

            userEntity = userRepository.save(userEntity);
            log.info("User id : {} updated successfully", id);

            // keep cache consistent
            userDetailsCacheService.save(userEntity);

            return buildUserDetailsResponse(userEntity);
        } catch (Exception exception) {
            log.error("Unable to update user id : {}. Exception : {}", id, exception.getMessage());
            throw new GlobalException(ErrorCodes.UNABLE_TO_UPDATE_USER);
        } finally {
            distributedLockService.releaseLock(lockKey, lockValue);
        }
    }

    private static void updateUserEntity(UserDetailsRequest userDetailsRequest, UserEntity userEntity) {
        userEntity.setName(userDetailsRequest.getName());
        userEntity.setAge(userDetailsRequest.getAge());
        userEntity.setCity(userDetailsRequest.getCity());
        userEntity.setUpdatedAt(LocalDateTime.now());
    }

    private UserDetailsResponse buildUserDetailsResponse(UserEntity userEntity) {
        return UserDetailsResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getName())
                .age(userEntity.getAge())
                .city(userEntity.getCity())
                .build();
    }

    private UserEntity buildUserEntityFromRequest(UserDetailsRequest userDetailsRequest) {
        return UserEntity.builder()
                .name(userDetailsRequest.getName())
                .age(userDetailsRequest.getAge())
                .city(userDetailsRequest.getCity())
                .build();
    }
}
