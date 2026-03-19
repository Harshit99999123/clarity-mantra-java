package com.clarity_mantra.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.clarity_mantra.core.enums.AuthProvider;
import com.clarity_mantra.core.entities.User;

class JwtServiceTest {

    @Test
    void createsAndParsesToken() {
        JwtService jwtService = new JwtService("12345678901234567890123456789012", 60);
        User user = new User();
        user.setId(123L);
        user.setEmail("user@example.com");
        user.setAuthProvider(AuthProvider.EMAIL);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(123L);
    }
}
