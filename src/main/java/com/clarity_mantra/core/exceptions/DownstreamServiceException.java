package com.clarity_mantra.core.exceptions;

import lombok.Getter;

@Getter
public class DownstreamServiceException extends RuntimeException {

    private final String service;
    private final int statusCode;
    private final boolean retryable;

    public DownstreamServiceException(String message, String service, int statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.service = service;
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public DownstreamServiceException(String message, String service, int statusCode, boolean retryable) {
        super(message);
        this.service = service;
        this.statusCode = statusCode;
        this.retryable = retryable;
    }
}
