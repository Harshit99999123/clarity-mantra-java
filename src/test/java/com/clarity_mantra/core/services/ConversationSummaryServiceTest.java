package com.clarity_mantra.core.services;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConversationSummaryServiceTest {

    private final ConversationSummaryService service = new ConversationSummaryService();

    @Test
    void buildsBoundedTitle() {
        String title = service.titleFrom("I feel uncertain about changing my career and I keep overthinking the risks.");
        assertThat(title).startsWith("I feel uncertain about changing my career");
    }

    @Test
    void buildsSummaryFromBothSides() {
        String summary = service.summaryFrom("I feel stuck.", "Focus on action without attachment.");
        assertThat(summary).contains("I feel stuck.").contains("Focus on action without attachment.");
    }
}
