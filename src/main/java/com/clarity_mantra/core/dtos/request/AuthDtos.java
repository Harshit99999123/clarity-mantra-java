package com.clarity_mantra.core.dtos.request;

import com.clarity_mantra.core.enums.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @Size(min = 8, max = 128) String password,
            String preferredLanguage) {
    }

    public record LoginRequest(
            @jakarta.validation.constraints.Email @NotBlank String email,
            @NotBlank String password) {
    }

    public record GoogleLoginRequest(
            @NotBlank String idToken,
            String preferredLanguage) {
    }

    public record AuthResponse(
            String accessToken,
            long expiresInSeconds,
            UserProfile user) {
    }

    public record UserProfile(
            Long id,
            String fullName,
            String email,
            String preferredLanguage,
            AuthProvider authProvider) {
    }
}
