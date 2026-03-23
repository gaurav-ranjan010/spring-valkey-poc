package com.example.spring_valkey_poc.exception;

import com.example.spring_valkey_poc.enums.ErrorCodes;
import lombok.Getter;
import lombok.Setter;

public class GlobalException extends RuntimeException{

    @Getter
    @Setter
    private int code;

    @Getter
    @Setter
    private String message;

    public GlobalException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public GlobalException(ErrorCodes errorCode){
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
}
