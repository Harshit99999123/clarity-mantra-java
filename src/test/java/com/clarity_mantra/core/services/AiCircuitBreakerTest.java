package com.clarity_mantra.core.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.clarity_mantra.core.configs.AiResilienceProperties;

class AiCircuitBreakerTest {

    @Test
    void opensAfterThresholdFailures() {
        AiResilienceProperties properties = new AiResilienceProperties(3, 100, 500, 0.25, 2, 1000);
        AiCircuitBreaker breaker = new AiCircuitBreaker(properties, Clock.fixed(Instant.parse("2026-03-20T00:00:00Z"), ZoneOffset.UTC));

        breaker.recordFailure();
        assertThat(breaker.allowRequest()).isTrue();

        breaker.recordFailure();
        assertThat(breaker.allowRequest()).isFalse();
    }
}
