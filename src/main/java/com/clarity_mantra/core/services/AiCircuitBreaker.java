package com.clarity_mantra.core.services;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.clarity_mantra.core.configs.AiResilienceProperties;

@Component
public class AiCircuitBreaker {

    private final AiResilienceProperties properties;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicLong openUntilEpochMillis = new AtomicLong(0);

    @Autowired
    public AiCircuitBreaker(AiResilienceProperties properties) {
        this(properties, Clock.systemUTC());
    }

    AiCircuitBreaker(AiResilienceProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public boolean allowRequest() {
        return clock.millis() >= openUntilEpochMillis.get();
    }

    public void recordSuccess() {
        consecutiveFailures.set(0);
        openUntilEpochMillis.set(0);
    }

    public void recordFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        if (failures >= properties.circuitFailureThreshold()) {
            openUntilEpochMillis.set(clock.millis() + properties.circuitOpenMillis());
        }
    }
}
