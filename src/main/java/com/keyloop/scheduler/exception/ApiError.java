package com.keyloop.scheduler.exception;

public class ApiError {

    public final String code;
    public final String message;

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
