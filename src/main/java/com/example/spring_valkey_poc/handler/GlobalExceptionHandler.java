package com.example.spring_valkey_poc.handler;

import com.example.spring_valkey_poc.exception.GlobalException;
import com.example.spring_valkey_poc.nonentity.BaseResponse;
import com.example.spring_valkey_poc.nonentity.ErrorDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<BaseResponse> handleGlobalException(GlobalException ex) {
        log.error("GlobalException occurred: code={}, message={}", ex.getCode(), ex.getMessage());
        BaseResponse baseResponse =  BaseResponse.builder()
                .success(false)
                .error(ErrorDetails.builder()
                               .code(ex.getCode())
                               .message(ex.getMessage())
                                   .build())
                           .build();
        return ResponseEntity.status(HttpStatusCode.valueOf(ex.getCode())).body(baseResponse);
    }
}
