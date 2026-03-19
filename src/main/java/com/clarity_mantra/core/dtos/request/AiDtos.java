package com.clarity_mantra.core.dtos.request;

import java.util.List;

public final class AiDtos {

    private AiDtos() {
    }

    public record ChatContextItem(String role, String message) {
    }

    public record ChatRequest(String message, List<ChatContextItem> context) {
    }

    public record Verse(String reference, String translation, List<String> themes) {
    }

    public record ChatResponse(
            String reflection,
            String reflection_question,
            List<Verse> verses) {
    }

    public record InsightRequest(List<ChatContextItem> conversation) {
    }

    public record InsightResponse(
            String quote,
            String meaning,
            String reflection,
            String shloka) {
    }
}
