package com.clarity_mantra.core.services;

import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;

import com.clarity_mantra.core.configs.AiResilienceProperties;

@Component
public class AiRetryPolicy {

    private final AiResilienceProperties properties;

    public AiRetryPolicy(AiResilienceProperties properties) {
        this.properties = properties;
    }

    public int maxAttempts() {
        return Math.max(1, properties.maxAttempts());
    }

    public long backoffWithJitterMillis(int attempt) {
        long exponential = properties.initialBackoffMillis() * (1L << Math.max(0, attempt - 1));
        long base = Math.min(properties.maxBackoffMillis(), exponential);
        double jitter = Math.max(0D, properties.jitterFactor());
        double multiplier = 1 - jitter + (ThreadLocalRandom.current().nextDouble() * jitter * 2);
        return Math.max(0L, Math.round(base * multiplier));
    }
}
