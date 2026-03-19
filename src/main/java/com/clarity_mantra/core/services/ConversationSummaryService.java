package com.clarity_mantra.core.services;

import org.springframework.stereotype.Service;

@Service
public class ConversationSummaryService {

    public String titleFrom(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return "Untitled Reflection";
        }
        return normalized.length() > 48 ? normalized.substring(0, 48).trim() + "..." : normalized;
    }

    public String summaryFrom(String userMessage, String mentorMessage) {
        String combined = normalize(userMessage) + " | " + normalize(mentorMessage);
        return combined.length() > 220 ? combined.substring(0, 220).trim() + "..." : combined;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
