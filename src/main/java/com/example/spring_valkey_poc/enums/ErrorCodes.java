package com.example.spring_valkey_poc.enums;

import lombok.Getter;

public enum ErrorCodes {

    USER_NOT_FOUND(404 , "User not found"),
    UNABLE_TO_ADD_USER(500 , "Unable to add user"),
    UNABLE_TO_UPDATE_USER(500 , "Unable to update user"),
    LOCK_ACQUISITION_FAILED(409 , "Could not acquire distributed lock. Please retry."),
    CACHE_WARM_UP_IN_PROGRESS(409 , "Cache warm-up is already running on another instance.");

    @Getter
    private final int code;

    @Getter
    private final String message;

    ErrorCodes(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
