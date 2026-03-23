package com.example.spring_valkey_poc.nonentity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserDetailsResponse {
    private long id;
    private String name;
    private int age;
    private String city;

}
