package com.keyloop.scheduler.exception;

import javax.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({SlotUnavailableException.class, IdempotencyConflictException.class})
    public ResponseEntity<ApiError> conflict(RuntimeException exception) {
        String code = exception instanceof SlotUnavailableException
                ? "SLOT_UNAVAILABLE" : "IDEMPOTENCY_KEY_REUSED";
        return error(HttpStatus.CONFLICT, code, exception.getMessage());
    }

    @ExceptionHandler(VehicleAlreadyBookedException.class)
    public ResponseEntity<ApiError> vehicleAlreadyBooked(VehicleAlreadyBookedException exception) {
        return error(HttpStatus.CONFLICT, "VEHICLE_ALREADY_BOOKED", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            MissingRequestHeaderException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> invalidInput(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed.");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(code, message));
    }
}
