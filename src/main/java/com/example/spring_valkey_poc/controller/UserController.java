package com.example.spring_valkey_poc.controller;

import com.example.spring_valkey_poc.nonentity.BaseResponse;
import com.example.spring_valkey_poc.nonentity.UserDetailsRequest;
import com.example.spring_valkey_poc.nonentity.UserDetailsResponse;
import com.example.spring_valkey_poc.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<UserDetailsResponse>> getUsersByName(@PathVariable("id") long id){

         UserDetailsResponse userDetailsResponse = userService.fetchUserById(id);

        BaseResponse<UserDetailsResponse> baseResponse = BaseResponse.<UserDetailsResponse>builder()
                                                                     .data(userDetailsResponse)
                                                                     .success(true)
                                                                     .build();
        return ResponseEntity.ok(baseResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BaseResponse<UserDetailsResponse>> updateUser(
            @PathVariable("id") long id,
            @RequestBody UserDetailsRequest userDetailsRequest) {

        UserDetailsResponse userDetailsResponse = userService.updateUser(id, userDetailsRequest);

        BaseResponse<UserDetailsResponse> baseResponse = BaseResponse.<UserDetailsResponse>builder()
                .data(userDetailsResponse)
                .success(true)
                .build();

        return ResponseEntity.ok(baseResponse);
    }
}
