package com.clarity_mantra.core.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.request.ConversationDtos;
import com.clarity_mantra.core.dtos.response.ApiResponse;
import com.clarity_mantra.core.security.AuthenticatedUser;
import com.clarity_mantra.core.security.CurrentUser;
import com.clarity_mantra.core.services.ConversationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations", description = "Conversation and message APIs")
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create conversation")
    public ResponseEntity<ApiResponse<ConversationDtos.ConversationResponse>> create(
            @CurrentUser AuthenticatedUser user,
            @RequestBody ConversationDtos.CreateConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.CONVERSATION_CREATED, conversationService.createConversation(user.id(), request)));
    }

    @GetMapping
    @Operation(summary = "List conversations")
    public ResponseEntity<ApiResponse<List<ConversationDtos.ConversationResponse>>> list(@CurrentUser AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.CONVERSATIONS_FETCHED, conversationService.listConversations(user.id())));
    }

    @GetMapping("/{conversationId}")
    @Operation(summary = "Get conversation detail")
    public ResponseEntity<ApiResponse<ConversationDtos.ConversationDetailResponse>> get(
            @CurrentUser AuthenticatedUser user,
            @PathVariable Long conversationId) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.CONVERSATION_FETCHED, conversationService.getConversation(user.id(), conversationId)));
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post message and get mentor response")
    public ResponseEntity<ApiResponse<ConversationDtos.MessageResponse>> message(
            @CurrentUser AuthenticatedUser user,
            @PathVariable Long conversationId,
            @Valid @RequestBody ConversationDtos.MessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.MESSAGE_POSTED, conversationService.postMessage(user.id(), conversationId, request)));
    }

    @PostMapping(path = "/{conversationId}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream mentor response with SSE")
    public SseEmitter stream(
            @CurrentUser AuthenticatedUser user,
            @PathVariable Long conversationId,
            @Valid @RequestBody ConversationDtos.MessageRequest request) {
        return conversationService.streamMessage(user.id(), conversationId, request);
    }
}
