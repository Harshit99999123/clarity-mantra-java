package com.clarity_mantra.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.request.AuthDtos;
import com.clarity_mantra.core.entities.User;
import com.clarity_mantra.core.enums.AuthProvider;
import com.clarity_mantra.core.exceptions.ValidationException;
import com.clarity_mantra.core.repositories.UserRepository;
import com.clarity_mantra.core.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleIdentityService googleIdentityService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void googleLoginCreatesUserFromVerifiedToken() {
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);
        when(googleIdentityService.verifyIdToken("id-token"))
                .thenReturn(new GoogleIdentityService.VerifiedGoogleUser("google-sub", "user@example.com", "Aarav Sharma"));
        when(userRepository.findByProviderSubject("google-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        AuthDtos.AuthResponse response = authService.googleLogin(new AuthDtos.GoogleLoginRequest("id-token", "en"));

        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getProviderSubject()).isEqualTo("google-sub");
        assertThat(savedUser.getEmail()).isEqualTo("user@example.com");
        assertThat(savedUser.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(response.accessToken()).isEqualTo("jwt-token");
    }

    @Test
    void googleLoginRejectsProviderSubjectConflict() {
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("user@example.com");
        existingUser.setProviderSubject("different-sub");
        existingUser.setAuthProvider(AuthProvider.EMAIL);

        when(googleIdentityService.verifyIdToken("id-token"))
                .thenReturn(new GoogleIdentityService.VerifiedGoogleUser("google-sub", "user@example.com", "Aarav Sharma"));
        when(userRepository.findByProviderSubject("google-sub")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.googleLogin(new AuthDtos.GoogleLoginRequest("id-token", "en")))
                .isInstanceOf(ValidationException.class)
                .hasMessage(MessageConstants.GOOGLE_ACCOUNT_LINK_CONFLICT);
    }
}
