package com.clarity_mantra.core.dtos.response;

import java.time.Instant;
import java.util.List;

import org.slf4j.MDC;

import com.clarity_mantra.core.constants.HeaderConstants;

public record ApiErrorResponse(
        boolean success,
        String message,
        List<ErrorDetail> errors,
        Instant timestamp,
        String correlationId) {

    public static ApiErrorResponse failure(String message, List<ErrorDetail> errors) {
        return new ApiErrorResponse(false, message, errors, Instant.now(), MDC.get(HeaderConstants.CORRELATION_ID));
    }
}
