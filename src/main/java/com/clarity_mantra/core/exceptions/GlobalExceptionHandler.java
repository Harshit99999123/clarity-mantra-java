package com.clarity_mantra.core.exceptions;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.response.ApiErrorResponse;
import com.clarity_mantra.core.dtos.response.ErrorDetail;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException exception) {
        log.warn("Business validation failed: {}", exception.getMessage());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", exception.getMessage());
    }

    @ExceptionHandler(DownstreamServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleDownstream(DownstreamServiceException exception) {
        HttpStatus status = exception.getStatusCode() >= 500 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        log.error("Downstream service failure service={} status={} retryable={}", exception.getService(), exception.getStatusCode(), exception.isRetryable());
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.failure(
                        status.getReasonPhrase(),
                        List.of(new ErrorDetail("DOWNSTREAM_" + exception.getService().toUpperCase().replace('-', '_'), exception.getMessage()))));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ErrorDetail> details = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error instanceof FieldError fieldError
                        ? new ErrorDetail("FIELD_VALIDATION_ERROR", fieldError.getField() + ": " + fieldError.getDefaultMessage())
                        : new ErrorDetail("VALIDATION_ERROR", error.getDefaultMessage()))
                .toList();
        log.warn("Request validation failed: {}", details);
        return ResponseEntity.badRequest().body(ApiErrorResponse.failure(MessageConstants.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleOther(Exception exception) {
        log.error("Unhandled exception", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", MessageConstants.UNEXPECTED_ERROR);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String code, String detail) {
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.failure(status.getReasonPhrase(), List.of(new ErrorDetail(code, detail))));
    }
}
