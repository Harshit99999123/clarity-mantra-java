package com.clarity_mantra.core.configs;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clarity.google")
public record GoogleAuthProperties(
        List<String> clientIds) {

    public boolean isConfigured() {
        return clientIds != null && clientIds.stream().anyMatch(id -> id != null && !id.isBlank());
    }
}
