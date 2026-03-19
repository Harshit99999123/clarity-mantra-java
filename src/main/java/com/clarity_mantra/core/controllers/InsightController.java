package com.clarity_mantra.core.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.response.ApiResponse;
import com.clarity_mantra.core.dtos.response.InsightDtos;
import com.clarity_mantra.core.security.AuthenticatedUser;
import com.clarity_mantra.core.security.CurrentUser;
import com.clarity_mantra.core.services.InsightService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/conversations/{conversationId}/insight-card")
@RequiredArgsConstructor
@Tag(name = "Insights", description = "Insight card APIs")
public class InsightController {

    private final InsightService insightService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate insight card")
    public ResponseEntity<ApiResponse<InsightDtos.InsightCardResponse>> generate(
            @CurrentUser AuthenticatedUser user,
            @PathVariable Long conversationId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.INSIGHT_GENERATED, insightService.generate(user.id(), conversationId)));
    }

    @GetMapping
    @Operation(summary = "Get latest insight card")
    public ResponseEntity<ApiResponse<InsightDtos.InsightCardResponse>> getLatest(
            @CurrentUser AuthenticatedUser user,
            @PathVariable Long conversationId) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.INSIGHT_FETCHED, insightService.getLatest(user.id(), conversationId)));
    }
}
