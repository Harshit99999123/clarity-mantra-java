package com.clarity_mantra.core.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Version;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.clarity_mantra.core.constants.HeaderConstants;
import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.constants.ServiceConstants;
import com.clarity_mantra.core.configs.AiProperties;
import com.clarity_mantra.core.dtos.request.AiDtos;
import com.clarity_mantra.core.exceptions.DownstreamServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AiServiceClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AiProperties properties;
    private final AiRetryPolicy aiRetryPolicy;
    private final AiCircuitBreaker aiCircuitBreaker;

    public AiServiceClient(
            ObjectMapper objectMapper,
            AiProperties properties,
            AiRetryPolicy aiRetryPolicy,
            AiCircuitBreaker aiCircuitBreaker) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.aiRetryPolicy = aiRetryPolicy;
        this.aiCircuitBreaker = aiCircuitBreaker;
        this.httpClient = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(properties.connectTimeoutSeconds()))
                .build();
    }

    public AiDtos.ChatResponse chat(AiDtos.ChatRequest request) {
        log.debug("Calling AI chat endpoint");
        return post("/ai/chat", request, AiDtos.ChatResponse.class);
    }

    public AiDtos.InsightResponse insight(AiDtos.InsightRequest request) {
        log.debug("Calling AI insight endpoint");
        return post("/ai/insight", request, AiDtos.InsightResponse.class);
    }

    public void streamChat(AiDtos.ChatRequest request, BiConsumer<String, String> eventConsumer) {
        ensureCircuitClosed();
        Instant start = Instant.now();
        try {
            HttpRequest httpRequest = buildRequest("/ai/chat/stream", request);
            HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() >= 400) {
                throw mapStatusFailure(response.statusCode(), MessageConstants.AI_STREAM_FAILURE, null);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                String currentEvent = "message";
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("event:")) {
                        currentEvent = line.substring(6).trim();
                    } else if (line.startsWith("data:")) {
                        eventConsumer.accept(currentEvent, line.substring(5).trim());
                    }
                }
            }
            aiCircuitBreaker.recordSuccess();
            log.info("AI stream completed in {} ms", Duration.between(start, Instant.now()).toMillis());
        } catch (DownstreamServiceException exception) {
            aiCircuitBreaker.recordFailure();
            throw exception;
        } catch (Exception exception) {
            aiCircuitBreaker.recordFailure();
            throw mapException(MessageConstants.AI_STREAM_FAILURE, exception);
        }
    }

    private <T> T post(String path, Object request, Class<T> responseType) {
        ensureCircuitClosed();
        DownstreamServiceException lastFailure = null;
        Instant start = Instant.now();

        for (int attempt = 1; attempt <= aiRetryPolicy.maxAttempts(); attempt++) {
            try {
                HttpRequest httpRequest = buildRequest(path, request);
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    DownstreamServiceException statusFailure = mapStatusFailure(response.statusCode(), MessageConstants.AI_REQUEST_FAILURE, response.body());
                    if (!statusFailure.isRetryable() || attempt == aiRetryPolicy.maxAttempts()) {
                        aiCircuitBreaker.recordFailure();
                        throw statusFailure;
                    }
                    lastFailure = statusFailure;
                    backoff(attempt, statusFailure);
                    continue;
                }
                T value = objectMapper.readValue(response.body(), responseType);
                aiCircuitBreaker.recordSuccess();
                log.info("AI request to {} completed in {} ms on attempt {}", path, Duration.between(start, Instant.now()).toMillis(), attempt);
                return value;
            } catch (DownstreamServiceException exception) {
                lastFailure = exception;
                if (!exception.isRetryable() || attempt == aiRetryPolicy.maxAttempts()) {
                    aiCircuitBreaker.recordFailure();
                    throw exception;
                }
                backoff(attempt, exception);
            } catch (Exception exception) {
                lastFailure = mapException(MessageConstants.AI_REQUEST_FAILURE, exception);
                if (!lastFailure.isRetryable() || attempt == aiRetryPolicy.maxAttempts()) {
                    aiCircuitBreaker.recordFailure();
                    throw lastFailure;
                }
                backoff(attempt, lastFailure);
            }
        }

        aiCircuitBreaker.recordFailure();
        throw lastFailure == null
                ? new DownstreamServiceException(MessageConstants.AI_REQUEST_FAILURE, ServiceConstants.AI_SERVICE, 503, true)
                : lastFailure;
    }

    private HttpRequest buildRequest(String path, Object request) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl() + path))
                .timeout(Duration.ofSeconds(properties.readTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header(HeaderConstants.REQUEST_SOURCE, ServiceConstants.CORE_SERVICE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)));

        String correlationId = MDC.get(HeaderConstants.CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()) {
            builder.header(HeaderConstants.CORRELATION_ID, correlationId);
        }
        return builder.build();
    }

    private void ensureCircuitClosed() {
        if (!aiCircuitBreaker.allowRequest()) {
            throw new DownstreamServiceException(MessageConstants.AI_CIRCUIT_OPEN, ServiceConstants.AI_SERVICE, 503, true);
        }
    }

    private void backoff(int attempt, DownstreamServiceException exception) {
        long delay = aiRetryPolicy.backoffWithJitterMillis(attempt);
        log.warn("Retrying AI request after failure attempt={} delayMs={} status={} retryable={}", attempt, delay, exception.getStatusCode(), exception.isRetryable());
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new DownstreamServiceException(MessageConstants.AI_REQUEST_FAILURE, ServiceConstants.AI_SERVICE, 503, true, interruptedException);
        }
    }

    private DownstreamServiceException mapStatusFailure(int statusCode, String message, String responseBody) {
        boolean retryable = statusCode == 408 || statusCode == 429 || statusCode >= 500;
        String detail = responseBody == null || responseBody.isBlank()
                ? message
                : message + " response=" + responseBody.substring(0, Math.min(500, responseBody.length()));
        return new DownstreamServiceException(detail, ServiceConstants.AI_SERVICE, statusCode, retryable);
    }

    private DownstreamServiceException mapException(String message, Exception exception) {
        boolean retryable = exception instanceof IOException
                || exception instanceof HttpTimeoutException
                || exception.getCause() instanceof IOException;
        return new DownstreamServiceException(message, ServiceConstants.AI_SERVICE, retryable ? 503 : 502, retryable, exception);
    }
}
