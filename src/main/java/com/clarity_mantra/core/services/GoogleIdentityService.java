package com.clarity_mantra.core.services;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.clarity_mantra.core.configs.GoogleAuthProperties;
import com.clarity_mantra.core.constants.MessageConstants;
import com.clarity_mantra.core.exceptions.ValidationException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GoogleIdentityService {

    private final GoogleAuthProperties googleAuthProperties;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public GoogleIdentityService(GoogleAuthProperties googleAuthProperties) {
        this.googleAuthProperties = googleAuthProperties;
        this.googleIdTokenVerifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(googleAuthProperties.clientIds())
                .build();
    }

    public VerifiedGoogleUser verifyIdToken(String idToken) {
        if (!googleAuthProperties.isConfigured()) {
            throw new ValidationException(MessageConstants.GOOGLE_AUTH_NOT_CONFIGURED);
        }

        try {
            GoogleIdToken googleIdToken = googleIdTokenVerifier.verify(idToken);
            if (googleIdToken == null) {
                throw new ValidationException(MessageConstants.INVALID_GOOGLE_ID_TOKEN);
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            Object emailVerified = payload.getEmailVerified();
            if (!Boolean.TRUE.equals(emailVerified)) {
                throw new ValidationException(MessageConstants.GOOGLE_EMAIL_NOT_VERIFIED);
            }

            VerifiedGoogleUser user = new VerifiedGoogleUser(
                    Objects.requireNonNull(payload.getSubject(), MessageConstants.INVALID_GOOGLE_ID_TOKEN),
                    Objects.requireNonNull(payload.getEmail(), MessageConstants.INVALID_GOOGLE_ID_TOKEN).toLowerCase(),
                    payload.get("name") == null ? payload.getEmail() : String.valueOf(payload.get("name")));
            log.info("Verified Google ID token for {}", user.email());
            return user;
        } catch (GeneralSecurityException | IOException exception) {
            log.warn("Google token verification failed", exception);
            throw new ValidationException(MessageConstants.INVALID_GOOGLE_ID_TOKEN);
        }
    }

    public record VerifiedGoogleUser(
            String subject,
            String email,
            String fullName) {
    }
}
