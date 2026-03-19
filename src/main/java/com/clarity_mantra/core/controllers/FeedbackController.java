package com.clarity_mantra.core.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.request.FeedbackDtos;
import com.clarity_mantra.core.dtos.response.ApiResponse;
import com.clarity_mantra.core.security.AuthenticatedUser;
import com.clarity_mantra.core.security.CurrentUser;
import com.clarity_mantra.core.services.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "Feedback APIs")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit product or session feedback")
    public ResponseEntity<ApiResponse<FeedbackDtos.FeedbackResponse>> submit(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody FeedbackDtos.FeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.FEEDBACK_SUBMITTED, feedbackService.submit(user.id(), request)));
    }
}
