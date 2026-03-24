package com.example.spring_valkey_poc.service;

import com.example.spring_valkey_poc.nonentity.UserDetailsRequest;
import com.example.spring_valkey_poc.nonentity.UserDetailsResponse;

public interface UserService {

    UserDetailsResponse fetchUserById(long id);

    UserDetailsResponse addUser(UserDetailsRequest userDetailsRequest);
}
