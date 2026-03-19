package com.clarity_mantra.core.dtos.response;

import java.time.Instant;

public final class InsightDtos {

    private InsightDtos() {
    }

    public record InsightCardResponse(
            Long id,
            String quote,
            String meaning,
            String reflectionPrompt,
            String shloka,
            Instant createdAt) {
    }
}
