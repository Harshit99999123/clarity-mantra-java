package com.clarity_mantra.core.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CrisisSupportServiceTest {

    private final CrisisSupportService service = new CrisisSupportService();

    @Test
    void flagsHighRiskLanguage() {
        assertThat(service.isHighRisk("Sometimes I want to end my life")).isTrue();
    }

    @Test
    void ignoresRegularReflectionLanguage() {
        assertThat(service.isHighRisk("I feel confused about my career")).isFalse();
    }
}
