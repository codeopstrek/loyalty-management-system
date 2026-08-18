package com.loyalty.loyaltyprogram.exception;

import com.loyalty.loyaltyprogram.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
		log.error("CustomerNotFoundException: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.OK).body(
				ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "Customer not found", List.of(ex.getMessage())));
	}

	@ExceptionHandler(CustomerAccountDeactivatedException.class)
	public ResponseEntity<ApiResponse<Object>> handleAccountDeactivated(CustomerAccountDeactivatedException ex) {
		log.error("CustomerAccountDeactivatedException: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.failure(HttpStatus.FORBIDDEN.value(),
				"Customer account inactive", List.of(ex.getMessage())));
	}

	@ExceptionHandler(PointsNotAvailableException.class)
	public ResponseEntity<ApiResponse<Object>> handlePointsNotAvailable(PointsNotAvailableException ex) {
		log.error("PointsNotAvailableException: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.OK).body(
				ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "Points not available", List.of(ex.getMessage())));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        log.error("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "Validation failed", errors));
    }

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
		log.error("Unhandled exception occurred", ex);
		return ResponseEntity.status(HttpStatus.OK).body(ApiResponse
				.failure(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong", List.of(ex.getMessage())));
	}
	
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiResponse<Object>> handleDuplicateResource(DuplicateResourceException ex) {
	    log.error("DuplicateResourceException: field={}, value={}", ex.getFieldName(), ex.getFieldValue());
	    return ResponseEntity.status(HttpStatus.OK)
	            .body(ApiResponse.failure(HttpStatus.CONFLICT.value(), "Duplicate value", List.of(ex.getMessage())));
	}
	
	@ExceptionHandler(TransactionNotFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleTransactionNotFound(TransactionNotFoundException ex) {
	    log.error("TransactionNotFoundException: {}", ex.getMessage());
	    return ResponseEntity.status(HttpStatus.NOT_FOUND)
	            .body(ApiResponse.failure(HttpStatus.NOT_FOUND.value(), "Transaction not found", List.of(ex.getMessage())));
	}
	
	@ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Object>> handleMalformedJson(
	        org.springframework.http.converter.HttpMessageNotReadableException ex) {
	    log.error("Malformed JSON request: {}", ex.getMessage());
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .body(ApiResponse.failure(HttpStatus.BAD_REQUEST.value(), "Invalid request body",
	                    List.of("Request body is malformed or contains invalid field types")));
	}
}