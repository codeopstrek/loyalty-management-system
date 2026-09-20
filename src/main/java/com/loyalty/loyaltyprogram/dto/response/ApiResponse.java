package com.loyalty.loyaltyprogram.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.slf4j.MDC;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

	private String requestId;
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private List<String> errors;
    private T data;

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return ApiResponse.<T>builder()
        		.requestId(MDC.get("requestId"))
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> failure(int status, String message, List<String> errors) {
        return ApiResponse.<T>builder()
        		.requestId(MDC.get("requestId"))
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .errors(errors == null ? Collections.emptyList() : errors)
                .build();
    }
}