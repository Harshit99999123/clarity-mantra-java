package com.clarity_mantra.core.services;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.request.AiDtos;
import com.clarity_mantra.core.dtos.request.ConversationDtos;
import com.clarity_mantra.core.enums.InputMode;
import com.clarity_mantra.core.enums.MessageRole;
import com.clarity_mantra.core.exceptions.NotFoundException;
import com.clarity_mantra.core.entities.ConversationMessage;
import com.clarity_mantra.core.entities.ConversationSession;
import com.clarity_mantra.core.entities.User;
import com.clarity_mantra.core.repositories.ConversationMessageRepository;
import com.clarity_mantra.core.repositories.ConversationSessionRepository;
import com.clarity_mantra.core.repositories.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationSessionRepository conversationSessionRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final UserRepository userRepository;
    private final AiServiceClient aiServiceClient;
    private final CrisisSupportService crisisSupportService;
    private final ConversationSummaryService conversationSummaryService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    @Qualifier("streamingTaskExecutor")
    private final TaskExecutor streamingTaskExecutor;

    @Transactional
    public ConversationDtos.ConversationResponse createConversation(Long userId, ConversationDtos.CreateConversationRequest request) {
        User user = getUser(userId);
        ConversationSession session = new ConversationSession();
        session.setUser(user);
        session.setTitle(request.title() == null || request.title().isBlank() ? "New Reflection" : request.title().trim());
        session.setLanguageCode(request.languageCode() == null || request.languageCode().isBlank()
                ? user.getPreferredLanguage()
                : request.languageCode().trim().toLowerCase());
        conversationSessionRepository.save(session);
        log.info("Created conversation {} for user {}", session.getId(), userId);
        return toConversationResponse(session);
    }

    @Transactional(readOnly = true)
    public List<ConversationDtos.ConversationResponse> listConversations(Long userId) {
        return conversationSessionRepository.findByUser_IdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toConversationResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationDtos.ConversationDetailResponse getConversation(Long userId, Long conversationId) {
        ConversationSession session = getConversationOwnedByUser(userId, conversationId);
        List<ConversationDtos.MessageResponse> messages = conversationMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
        return new ConversationDtos.ConversationDetailResponse(toConversationResponse(session), messages);
    }

    public ConversationDtos.MessageResponse postMessage(Long userId, Long conversationId, ConversationDtos.MessageRequest request) {
        getConversationOwnedByUser(userId, conversationId);
        AiDtos.ChatRequest chatRequest = buildChatRequest(conversationId, request.message());
        saveUserMessage(conversationId, request);

        AiDtos.ChatResponse aiResponse = crisisSupportService.isHighRisk(request.message())
                ? crisisSupportService.safeResponse()
                : aiServiceClient.chat(chatRequest);

        ConversationMessage mentorMessage = saveMentorMessage(conversationId, aiResponse, request.inputMode(), request.message());
        log.info("Generated mentor response for conversation {}", conversationId);
        return toMessageResponse(mentorMessage);
    }

    public SseEmitter streamMessage(Long userId, Long conversationId, ConversationDtos.MessageRequest request) {
        getConversationOwnedByUser(userId, conversationId);
        AiDtos.ChatRequest chatRequest = buildChatRequest(conversationId, request.message());
        saveUserMessage(conversationId, request);

        SseEmitter emitter = new SseEmitter(120_000L);
        if (crisisSupportService.isHighRisk(request.message())) {
            AiDtos.ChatResponse safe = crisisSupportService.safeResponse();
            try {
                emitter.send(SseEmitter.event().name("token").data(objectMapper.writeValueAsString(Collections.singletonMap("text", safe.reflection()))));
                emitter.send(SseEmitter.event().name("done").data("{}"));
            } catch (IOException exception) {
                emitter.completeWithError(exception);
                return emitter;
            }
            saveMentorMessage(conversationId, safe, request.inputMode(), request.message());
            emitter.complete();
            return emitter;
        }

        StringBuilder fullText = new StringBuilder();
        StringBuilder metaJson = new StringBuilder();

        streamingTaskExecutor.execute(() -> {
            try {
                aiServiceClient.streamChat(chatRequest, (event, data) -> {
                    try {
                        if ("meta".equals(event)) {
                            metaJson.setLength(0);
                            metaJson.append(data);
                        } else if ("token".equals(event)) {
                            String text = objectMapper.readTree(data).path("text").asText("");
                            fullText.append(text);
                        }
                        emitter.send(SseEmitter.event().name(event).data(data));
                    } catch (Exception exception) {
                        throw new IllegalStateException(exception);
                    }
                });

                emitter.send(SseEmitter.event().name("done").data("{}"));
                persistStreamedMentorMessage(conversationId, request, fullText.toString().trim(), metaJson.length() == 0 ? null : metaJson.toString());
                log.info("Completed streaming mentor response for conversation {}", conversationId);
                emitter.complete();
            } catch (Exception exception) {
                log.error("Streaming failed for conversation {}", conversationId, exception);
                emitter.completeWithError(exception);
            }
        });

        return emitter;
    }

    private ConversationMessage saveUserMessage(Long conversationId, ConversationDtos.MessageRequest request) {
        return transactionTemplate.execute(status -> {
            ConversationMessage userMessage = new ConversationMessage();
            userMessage.setConversation(conversationSessionRepository.getReferenceById(conversationId));
            userMessage.setRole(MessageRole.USER);
            userMessage.setInputMode(request.inputMode());
            userMessage.setContent(request.message().trim());
            return conversationMessageRepository.save(userMessage);
        });
    }

    private ConversationMessage saveMentorMessage(Long conversationId, AiDtos.ChatResponse aiResponse, InputMode inputMode, String userText) {
        return transactionTemplate.execute(status -> {
            ConversationSession session = conversationSessionRepository.findById(conversationId)
                    .orElseThrow(() -> new NotFoundException(MessageConstants.CONVERSATION_NOT_FOUND));
            ConversationMessage mentorMessage = new ConversationMessage();
            mentorMessage.setConversation(session);
            mentorMessage.setRole(MessageRole.MENTOR);
            mentorMessage.setInputMode(inputMode);
            mentorMessage.setContent(aiResponse.reflection());
            mentorMessage.setReflectionQuestion(aiResponse.reflection_question());
            try {
                mentorMessage.setReferencesJson(objectMapper.writeValueAsString(aiResponse.verses()));
            } catch (Exception exception) {
                throw new IllegalStateException(MessageConstants.VERSES_STORE_FAILURE, exception);
            }
            ConversationMessage saved = conversationMessageRepository.save(mentorMessage);
            refreshConversationMetadata(session, userText, aiResponse.reflection());
            conversationSessionRepository.save(session);
            return saved;
        });
    }

    private void persistStreamedMentorMessage(Long conversationId, ConversationDtos.MessageRequest request, String mentorText, String metaJson) {
        transactionTemplate.executeWithoutResult(status -> {
            ConversationSession latestSession = conversationSessionRepository.findById(conversationId)
                    .orElseThrow(() -> new NotFoundException(MessageConstants.CONVERSATION_NOT_FOUND));
            ConversationMessage mentorMessage = new ConversationMessage();
            mentorMessage.setConversation(latestSession);
            mentorMessage.setRole(MessageRole.MENTOR);
            mentorMessage.setInputMode(request.inputMode());
            mentorMessage.setContent(mentorText);
            mentorMessage.setReferencesJson(metaJson);
            conversationMessageRepository.save(mentorMessage);
            refreshConversationMetadata(latestSession, request.message(), mentorText);
            conversationSessionRepository.save(latestSession);
        });
    }

    private AiDtos.ChatRequest buildChatRequest(Long conversationId, String message) {
        List<AiDtos.ChatContextItem> context = conversationMessageRepository.findTop10ByConversation_IdOrderByCreatedAtDesc(conversationId)
                .stream()
                .sorted((left, right) -> left.getCreatedAt().compareTo(right.getCreatedAt()))
                .map(item -> new AiDtos.ChatContextItem(item.getRole() == MessageRole.USER ? "user" : "assistant", item.getContent()))
                .toList();
        return new AiDtos.ChatRequest(message, context);
    }

    private void refreshConversationMetadata(ConversationSession session, String userText, String mentorText) {
        if (session.getSummary() == null || session.getSummary().isBlank()) {
            session.setTitle(conversationSummaryService.titleFrom(userText));
        }
        session.setSummary(conversationSummaryService.summaryFrom(userText, mentorText));
    }

    private ConversationSession getConversationOwnedByUser(Long userId, Long conversationId) {
        return conversationSessionRepository.findByIdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new NotFoundException(MessageConstants.CONVERSATION_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException(MessageConstants.USER_NOT_FOUND));
    }

    private ConversationDtos.ConversationResponse toConversationResponse(ConversationSession session) {
        return new ConversationDtos.ConversationResponse(
                session.getId(),
                session.getTitle(),
                session.getSummary(),
                session.getLanguageCode(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getUpdatedAt());
    }

    private ConversationDtos.MessageResponse toMessageResponse(ConversationMessage message) {
        List<ConversationDtos.Reference> references = parseReferences(message.getReferencesJson());
        return new ConversationDtos.MessageResponse(
                message.getId(),
                message.getRole(),
                message.getInputMode(),
                message.getContent(),
                message.getReflectionQuestion(),
                references,
                message.getCreatedAt());
    }

    private List<ConversationDtos.Reference> parseReferences(String referencesJson) {
        if (referencesJson == null || referencesJson.isBlank()) {
            return List.of();
        }
        try {
            if (referencesJson.contains("\"references\"")) {
                return objectMapper.readTree(referencesJson)
                        .path("references")
                        .findValuesAsText("reference")
                        .stream()
                        .map(reference -> new ConversationDtos.Reference(reference, null, List.of()))
                        .toList();
            }
            List<AiDtos.Verse> verses = objectMapper.readValue(referencesJson, new TypeReference<List<AiDtos.Verse>>() {
            });
            return verses.stream().map(verse -> new ConversationDtos.Reference(verse.reference(), verse.translation(), verse.themes())).toList();
        } catch (Exception exception) {
            return List.of();
        }
    }
}
