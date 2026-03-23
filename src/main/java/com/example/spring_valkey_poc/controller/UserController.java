package com.example.spring_valkey_poc.controller;

import com.example.spring_valkey_poc.nonentity.BaseResponse;
import com.example.spring_valkey_poc.nonentity.UserDetailsRequest;
import com.example.spring_valkey_poc.nonentity.UserDetailsResponse;
import com.example.spring_valkey_poc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<BaseResponse<UserDetailsResponse>> addUserToDb(@RequestBody UserDetailsRequest userDetailsRequest){

        UserDetailsResponse userDetailsResponse = userService.addUser(userDetailsRequest);

        BaseResponse<UserDetailsResponse> baseResponse = BaseResponse.<UserDetailsResponse>builder()
                .data(userDetailsResponse)
                .success(true)
                .build();

        return ResponseEntity.ok(baseResponse);
    }

    @GetMapping("/{name}")
    public ResponseEntity<BaseResponse<List<UserDetailsResponse>>> getUsersByName(@PathVariable("name") String name){

         List<UserDetailsResponse> userDetailsResponseList = userService.fetchUserByName(name);

        BaseResponse<List<UserDetailsResponse>> baseResponse = BaseResponse.<List<UserDetailsResponse>>builder()
                                                                     .data(userDetailsResponseList)
                                                                     .success(true)
                                                                     .build();
        return ResponseEntity.ok(baseResponse);
    }
}
