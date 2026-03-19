package com.clarity_mantra.core.security;

public record AuthenticatedUser(
        Long id,
        String email) {
}
