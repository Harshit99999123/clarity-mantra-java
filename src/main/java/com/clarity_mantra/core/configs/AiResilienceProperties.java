package com.clarity_mantra.core.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clarity.ai.resilience")
public record AiResilienceProperties(
        int maxAttempts,
        long initialBackoffMillis,
        long maxBackoffMillis,
        double jitterFactor,
        int circuitFailureThreshold,
        long circuitOpenMillis) {
}
