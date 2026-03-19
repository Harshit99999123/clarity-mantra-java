package com.clarity_mantra.core.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.dtos.request.AuthDtos;
import com.clarity_mantra.core.enums.AuthProvider;
import com.clarity_mantra.core.exceptions.NotFoundException;
import com.clarity_mantra.core.exceptions.ValidationException;
import com.clarity_mantra.core.entities.User;
import com.clarity_mantra.core.repositories.UserRepository;
import com.clarity_mantra.core.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdentityService googleIdentityService;

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        userRepository.findByEmailIgnoreCase(request.email()).ifPresent(user -> {
            throw new ValidationException(MessageConstants.EMAIL_ALREADY_REGISTERED);
        });

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthProvider(AuthProvider.EMAIL);
        user.setPreferredLanguage(normalizeLanguage(request.preferredLanguage()));
        userRepository.save(user);
        log.info("Registered new email user for {}", user.getEmail());
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ValidationException(MessageConstants.INVALID_CREDENTIALS));
        if (user.getAuthProvider() != AuthProvider.EMAIL || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ValidationException(MessageConstants.INVALID_CREDENTIALS);
        }
        log.info("Authenticated email user {}", user.getEmail());
        return toAuthResponse(user);
    }

    @Transactional
    public AuthDtos.AuthResponse googleLogin(AuthDtos.GoogleLoginRequest request) {
        GoogleIdentityService.VerifiedGoogleUser verifiedGoogleUser = googleIdentityService.verifyIdToken(request.idToken());
        String email = verifiedGoogleUser.email().trim().toLowerCase();
        String subject = verifiedGoogleUser.subject().trim();

        User user = userRepository.findByProviderSubject(subject)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(email).orElse(null));

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setFullName(verifiedGoogleUser.fullName().trim());
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setProviderSubject(subject);
            user.setPreferredLanguage(normalizeLanguage(request.preferredLanguage()));
            userRepository.save(user);
            log.info("Created Google-backed user {}", user.getEmail());
        } else {
            if (!user.getEmail().equalsIgnoreCase(email)) {
                throw new ValidationException(MessageConstants.GOOGLE_ACCOUNT_LINK_CONFLICT);
            }
            if (user.getProviderSubject() != null && !user.getProviderSubject().equals(subject)) {
                throw new ValidationException(MessageConstants.GOOGLE_ACCOUNT_LINK_CONFLICT);
            }
            user.setFullName(verifiedGoogleUser.fullName().trim());
            if (user.getProviderSubject() == null || user.getProviderSubject().isBlank()) {
                user.setProviderSubject(subject);
            }
            if (user.getAuthProvider() == null) {
                user.setAuthProvider(AuthProvider.GOOGLE);
            }
            user.setPreferredLanguage(normalizeLanguage(request.preferredLanguage()));
            userRepository.save(user);
            log.info("Authenticated Google-backed user {}", user.getEmail());
        }

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserProfile getProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException(MessageConstants.USER_NOT_FOUND));
        return toProfile(user);
    }

    private AuthDtos.AuthResponse toAuthResponse(User user) {
        return new AuthDtos.AuthResponse(jwtService.generateToken(user), jwtService.getExpirationSeconds(), toProfile(user));
    }

    private AuthDtos.UserProfile toProfile(User user) {
        return new AuthDtos.UserProfile(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPreferredLanguage(),
                user.getAuthProvider());
    }

    private String normalizeLanguage(String preferredLanguage) {
        if (preferredLanguage == null || preferredLanguage.isBlank()) {
            return "en";
        }
        return preferredLanguage.trim().toLowerCase();
    }
}
