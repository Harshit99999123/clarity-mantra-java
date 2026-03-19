package com.clarity_mantra.core.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.request.AuthDtos;
import com.clarity_mantra.core.dtos.response.ApiResponse;
import com.clarity_mantra.core.security.AuthenticatedUser;
import com.clarity_mantra.core.security.CurrentUser;
import com.clarity_mantra.core.services.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and profile APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register with email and password")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(MessageConstants.AUTH_REGISTERED, authService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.AUTH_LOGGED_IN, authService.login(request)));
    }

    @PostMapping("/google")
    @Operation(summary = "Login with Google ID token")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> google(@Valid @RequestBody AuthDtos.GoogleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.AUTH_LOGGED_IN, authService.googleLogin(request)));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<AuthDtos.UserProfile>> me(@CurrentUser AuthenticatedUser user) {
        return ResponseEntity.ok(ApiResponse.success(MessageConstants.AUTH_PROFILE_FETCHED, authService.getProfile(user.id())));
    }
}
