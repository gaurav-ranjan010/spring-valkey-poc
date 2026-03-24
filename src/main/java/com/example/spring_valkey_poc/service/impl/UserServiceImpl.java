package com.example.spring_valkey_poc.service.impl;

import com.example.spring_valkey_poc.cache.service.UserDetailsCacheService;
import com.example.spring_valkey_poc.entity.UserEntity;
import com.example.spring_valkey_poc.enums.ErrorCodes;
import com.example.spring_valkey_poc.exception.GlobalException;
import com.example.spring_valkey_poc.nonentity.UserDetailsRequest;
import com.example.spring_valkey_poc.nonentity.UserDetailsResponse;
import com.example.spring_valkey_poc.records.UserRecord;
import com.example.spring_valkey_poc.repository.UserRepository;
import com.example.spring_valkey_poc.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserDetailsCacheService userDetailsCacheService;

    @Override
    public UserDetailsResponse fetchUserById(long id) {
        try{
            log.info("Received request to fetch user details for name : {}", id);
            Optional<UserEntity> userEntityOptional = fetchUserEntityFromCache(id);
            if(userEntityOptional.isPresent()){
                log.info("User details fetched successfully from cache for name : {}. User details : {}", id, userEntityOptional.get());
                return buildUserDetailsResponseFromCache(userEntityOptional.get());
            }

            userEntityOptional= userRepository.findById(id);
            if(userEntityOptional.isEmpty()){
                log.warn("No user found with name : {}", id);
                throw new GlobalException(ErrorCodes.USER_NOT_FOUND);
            }
            userDetailsCacheService.save(userEntityOptional.get());
            log.info("Successfully saved record with id :: {} in cache" , userEntityOptional.get().getId());
            UserDetailsResponse userDetailsResponseList = buildUserDetailsResponseFromCache(userEntityOptional.get());
            log.info("User details fetched successfully for name : {}. User details : {}", id, userDetailsResponseList);
            return userDetailsResponseList;
        }
        catch (Exception exception){
            log.error("Unable to fetch user details for name : {}. Exception : {}", id, exception.getMessage());
            throw new GlobalException(ErrorCodes.USER_NOT_FOUND);
        }
    }

    @Override
    public UserDetailsResponse addUser(UserDetailsRequest userDetailsRequest) {
        log.info("Received request to add user with details : {}", userDetailsRequest);
        try{
            UserEntity userEntity = buildUserEntityFromRequest(userDetailsRequest);
            userEntity = userRepository.save(userEntity);
            log.info("User added successfully with id : {}", userEntity.getId());
            return buildUserDetailsResponseFromEntity(userEntity);
        }
        catch (Exception exception){
            log.error("Unable to add user with details : {}. Exception : {}", userDetailsRequest, exception.getMessage());
            throw new GlobalException(ErrorCodes.UNABLE_TO_ADD_USER);
        }
    }

    private Optional<UserEntity> fetchUserEntityFromCache(long id){
        log.info("Attempting to fetch user details from cache for id : {}", id);
        Optional<UserEntity> userEntityOptional = userDetailsCacheService.findById(id);
        if(userEntityOptional.isPresent()){
            log.info("User details found in cache for id : {}. User details : {}", id, userEntityOptional.get());
            return userEntityOptional;
        }
        return Optional.empty();
    }

    private UserDetailsResponse buildUserDetailsResponseFromRecord(UserRecord userRecord){
        return UserDetailsResponse.builder()
                .id(userRecord.id())
                .name(userRecord.name())
                .age(userRecord.age())
                .city(userRecord.city())
                .build();
    }

    private UserDetailsResponse buildUserDetailsResponseFromCache(UserEntity userEntity){
        return UserDetailsResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getName())
                .age(userEntity.getAge())
                .city(userEntity.getCity())
                .build();
    }

    private UserDetailsResponse buildUserDetailsResponseFromEntity(UserEntity userEntity){
        return UserDetailsResponse.builder()
                .id(userEntity.getId())
                .name(userEntity.getName())
                .age(userEntity.getAge())
                .city(userEntity.getCity())
                .build();
    }

    private UserEntity buildUserEntityFromRequest(UserDetailsRequest userDetailsRequest){
        return UserEntity.builder()
                .name(userDetailsRequest.getName())
                .age(userDetailsRequest.getAge())
                .city(userDetailsRequest.getCity())
                .build();
    }
}
