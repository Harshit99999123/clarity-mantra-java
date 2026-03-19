package com.clarity_mantra.core.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.request.AiDtos;
import com.clarity_mantra.core.dtos.response.InsightDtos;
import com.clarity_mantra.core.enums.MessageRole;
import com.clarity_mantra.core.exceptions.NotFoundException;
import com.clarity_mantra.core.entities.ConversationSession;
import com.clarity_mantra.core.entities.InsightCard;
import com.clarity_mantra.core.repositories.ConversationMessageRepository;
import com.clarity_mantra.core.repositories.ConversationSessionRepository;
import com.clarity_mantra.core.repositories.InsightCardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsightService {

    private final InsightCardRepository insightCardRepository;
    private final ConversationSessionRepository conversationSessionRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final AiServiceClient aiServiceClient;
    private final TransactionTemplate transactionTemplate;

    public InsightDtos.InsightCardResponse generate(Long userId, Long conversationId) {
        getConversation(userId, conversationId);
        List<AiDtos.ChatContextItem> conversation = conversationMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(message -> new AiDtos.ChatContextItem(message.getRole() == MessageRole.USER ? "user" : "assistant", message.getContent()))
                .toList();
        AiDtos.InsightResponse aiResponse = aiServiceClient.insight(new AiDtos.InsightRequest(conversation));

        InsightCard card = transactionTemplate.execute(status -> {
            InsightCard created = new InsightCard();
            created.setConversation(conversationSessionRepository.getReferenceById(conversationId));
            created.setQuote(aiResponse.quote());
            created.setMeaning(aiResponse.meaning());
            created.setReflectionPrompt(aiResponse.reflection());
            created.setShloka(aiResponse.shloka());
            return insightCardRepository.save(created);
        });
        log.info("Generated insight card {} for conversation {}", card.getId(), conversationId);
        return toResponse(card);
    }

    @Transactional(readOnly = true)
    public InsightDtos.InsightCardResponse getLatest(Long userId, Long conversationId) {
        getConversation(userId, conversationId);
        InsightCard card = insightCardRepository.findTopByConversation_IdOrderByCreatedAtDesc(conversationId)
                .orElseThrow(() -> new NotFoundException(MessageConstants.INSIGHT_NOT_FOUND));
        return toResponse(card);
    }

    private ConversationSession getConversation(Long userId, Long conversationId) {
        return conversationSessionRepository.findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new NotFoundException(MessageConstants.CONVERSATION_NOT_FOUND));
    }

    private InsightDtos.InsightCardResponse toResponse(InsightCard card) {
        return new InsightDtos.InsightCardResponse(
                card.getId(),
                card.getQuote(),
                card.getMeaning(),
                card.getReflectionPrompt(),
                card.getShloka(),
                card.getCreatedAt());
    }
}
