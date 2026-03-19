package com.clarity_mantra.core.dtos.request;

import java.time.Instant;

import com.clarity_mantra.core.enums.FeedbackType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public final class FeedbackDtos {

    private FeedbackDtos() {
    }

    public record FeedbackRequest(
            Long conversationId,
            FeedbackType type,
            @Min(1) @Max(5) Integer rating,
            @Size(max = 2000) String comment) {
    }

    public record FeedbackResponse(
            Long id,
            FeedbackType type,
            Integer rating,
            String comment,
            Long conversationId,
            Instant createdAt) {
    }
}
