package com.example.spring_valkey_poc.service.impl;

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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<UserDetailsResponse> fetchUserByName(String name) {
        try{
            log.info("Received request to fetch user details for name : {}", name);
            List<UserRecord> userRecordList = userRepository.findByName(name);
            if(userRecordList.isEmpty()){
                log.warn("No user found with name : {}", name);
                throw new GlobalException(ErrorCodes.USER_NOT_FOUND);
            }

            List<UserDetailsResponse> userDetailsResponseList = userRecordList.stream()
                    .map(this::buildUserDetailsResponseFromRecord)
                    .toList();
            log.info("User details fetched successfully for name : {}. User details : {}", name, userDetailsResponseList);
            return userDetailsResponseList;
        }
        catch (Exception exception){
            log.error("Unable to fetch user details for name : {}. Exception : {}", name, exception.getMessage());
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

    private UserDetailsResponse buildUserDetailsResponseFromRecord(UserRecord userRecord){
        return UserDetailsResponse.builder()
                .id(userRecord.id())
                .name(userRecord.name())
                .age(userRecord.age())
                .city(userRecord.city())
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
