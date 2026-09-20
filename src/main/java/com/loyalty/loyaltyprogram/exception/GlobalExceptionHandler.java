package com.loyalty.loyaltyprogram.exception;

import com.loyalty.loyaltyprogram.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomerNotFound(CustomerNotFoundException ex) {
        log.atError()
                .setMessage("Customer search failed: Customer not found")
                .addKeyValue("exceptionClass", ex.getClass().getSimpleName())
                .addKeyValue("errorDetail", ex.getMessage())
                .log();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "Customer not found", List.of(ex.getMessage())));
    }

    @ExceptionHandler(CustomerAccountDeactivatedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountDeactivated(CustomerAccountDeactivatedException ex) {
        log.atError()
                .setMessage("Blocked attempt: Customer account inactive")
                .addKeyValue("exceptionClass", ex.getClass().getSimpleName())
                .addKeyValue("errorDetail", ex.getMessage())
                .log();

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.failure(HttpStatus.FORBIDDEN.value(), "Customer account inactive", List.of(ex.getMessage())));
    }

    @ExceptionHandler(PointsNotAvailableException.class)
    public ResponseEntity<ApiResponse<Object>> handlePointsNotAvailable(PointsNotAvailableException ex) {
        log.atError()
                .setMessage("Transaction failed: Points not available")
                .addKeyValue("exceptionClass", ex.getClass().getSimpleName())
                .addKeyValue("errorDetail", ex.getMessage())
                .log();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "Points not available", List.of(ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        log.atError()
                .setMessage("Validation failed for request payload")
                .addKeyValue("exceptionClass", ex.getClass().getSimpleName())
                .addKeyValue("validationErrors", errors)
                .log();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "Validation failed", errors));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResource(DuplicateResourceException ex) {
        log.atError()
                .setMessage("Duplicate resource encountered")
                .addKeyValue("exceptionClass", ex.getClass().getSimpleName())
                .addKeyValue("fieldName", ex.getFieldName())
                .addKeyValue("fieldValue", ex.getFieldValue())
                .addKeyValue("errorDetail", ex.getMessage())
                .log();

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(HttpStatus.CONFLICT.value(), "Duplicate value", List.of(ex.getMessage())));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleTransactionNotFound(TransactionNotFoundException ex) {
        log.atError()
                .setMessage("Transaction lookup failed: Transaction not found")
                .addKeyValue("exceptionClass", ex.getClass().getSimpleName())
                .addKeyValue("errorDetail", ex.getMessage())
                .log();

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "Transaction not found", List.of(ex.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMalformedJson(HttpMessageNotReadableException ex) {
        log.atError()
                .setMessage("Malformed JSON request body")
                .addKeyValue("exceptionClass", ex.getClass().getSimpleName())
                .addKeyValue("errorDetail", ex.getMessage())
                .log();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "Invalid request body",
                        List.of("Request body is malformed or contains invalid field types")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        log.atError()
                .setCause(ex)
                .setMessage("Unhandled internal exception occurred")
                .addKeyValue("exceptionClass", ex.getClass().getSimpleName())
                .addKeyValue("errorDetail", ex.getMessage())
                .log();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong", List.of(ex.getMessage())));
    }
}