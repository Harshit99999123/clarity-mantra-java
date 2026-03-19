package com.clarity_mantra.core.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.clarity_mantra.core.configs.AiResilienceProperties;

class AiRetryPolicyTest {

    @Test
    void backoffStaysWithinConfiguredBounds() {
        AiRetryPolicy policy = new AiRetryPolicy(new AiResilienceProperties(3, 100, 500, 0.25, 5, 30000));

        long first = policy.backoffWithJitterMillis(1);
        long second = policy.backoffWithJitterMillis(2);

        assertThat(first).isBetween(75L, 125L);
        assertThat(second).isBetween(150L, 250L);
    }
}
