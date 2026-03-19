package com.clarity_mantra.core.dtos.request;

import java.time.Instant;
import java.util.List;

import com.clarity_mantra.core.enums.ConversationStatus;
import com.clarity_mantra.core.enums.InputMode;
import com.clarity_mantra.core.enums.MessageRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ConversationDtos {

    private ConversationDtos() {
    }

    public record CreateConversationRequest(String title, String languageCode) {
    }

    public record ConversationResponse(
            Long id,
            String title,
            String summary,
            String languageCode,
            ConversationStatus status,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record MessageRequest(
            @NotBlank @Size(max = 4000) String message,
            @NotNull InputMode inputMode) {
    }

    public record Reference(String reference, String translation, List<String> themes) {
    }

    public record MessageResponse(
            Long id,
            MessageRole role,
            InputMode inputMode,
            String content,
            String reflectionQuestion,
            List<Reference> references,
            Instant createdAt) {
    }

    public record ConversationDetailResponse(
            ConversationResponse conversation,
            List<MessageResponse> messages) {
    }
}
