package com.keyloop.scheduler.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("The idempotency key was already used with a different request.");
    }
}
