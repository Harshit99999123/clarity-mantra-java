package com.clarity_mantra.core.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clarity.ai")
public record AiProperties(
        String baseUrl,
        int connectTimeoutSeconds,
        int readTimeoutSeconds) {
}
