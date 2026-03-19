package com.clarity_mantra.core.dtos.response;

import java.time.Instant;

import org.slf4j.MDC;

import com.clarity_mantra.core.constants.HeaderConstants;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp,
        String correlationId) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now(), MDC.get(HeaderConstants.CORRELATION_ID));
    }
}
