package com.example.spring_valkey_poc.service;

import com.example.spring_valkey_poc.nonentity.UserDetailsRequest;
import com.example.spring_valkey_poc.nonentity.UserDetailsResponse;

import java.util.List;

public interface UserService {

    List<UserDetailsResponse> fetchUserByName(String name);

    UserDetailsResponse addUser(UserDetailsRequest userDetailsRequest);
}
