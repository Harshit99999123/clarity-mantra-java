package com.clarity_mantra.core.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.request.AiDtos;

@Service
public class CrisisSupportService {

    private static final List<String> HIGH_RISK_TERMS = List.of(
            "suicide",
            "kill myself",
            "self harm",
            "end my life",
            "hurt myself",
            "don't want to live");

    public boolean isHighRisk(String message) {
        String normalized = message == null ? "" : message.toLowerCase();
        return HIGH_RISK_TERMS.stream().anyMatch(normalized::contains);
    }

    public AiDtos.ChatResponse safeResponse() {
        return new AiDtos.ChatResponse(
                MessageConstants.HIGH_RISK_SAFE_RESPONSE,
                MessageConstants.HIGH_RISK_REFLECTION,
                List.of());
    }
}
