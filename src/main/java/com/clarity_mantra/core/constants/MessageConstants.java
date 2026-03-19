package com.clarity_mantra.core.constants;

public final class MessageConstants {

    public static final String HEALTH_OK = "Service is healthy.";
    public static final String AUTH_REGISTERED = "User registered successfully.";
    public static final String AUTH_LOGGED_IN = "User authenticated successfully.";
    public static final String AUTH_PROFILE_FETCHED = "Profile fetched successfully.";
    public static final String CONVERSATION_CREATED = "Conversation created successfully.";
    public static final String CONVERSATIONS_FETCHED = "Conversations fetched successfully.";
    public static final String CONVERSATION_FETCHED = "Conversation fetched successfully.";
    public static final String MESSAGE_POSTED = "Mentor response generated successfully.";
    public static final String INSIGHT_GENERATED = "Insight card generated successfully.";
    public static final String INSIGHT_FETCHED = "Insight card fetched successfully.";
    public static final String FEEDBACK_SUBMITTED = "Feedback submitted successfully.";

    public static final String USER_NOT_FOUND = "User not found.";
    public static final String CONVERSATION_NOT_FOUND = "Conversation not found.";
    public static final String INSIGHT_NOT_FOUND = "Insight card not found.";
    public static final String EMAIL_ALREADY_REGISTERED = "Email is already registered.";
    public static final String INVALID_CREDENTIALS = "Invalid credentials.";
    public static final String GOOGLE_AUTH_NOT_CONFIGURED = "Google authentication is not configured on the server.";
    public static final String INVALID_GOOGLE_ID_TOKEN = "Invalid Google ID token.";
    public static final String GOOGLE_EMAIL_NOT_VERIFIED = "Google account email is not verified.";
    public static final String GOOGLE_ACCOUNT_LINK_CONFLICT = "Google account could not be linked to this user.";
    public static final String VALIDATION_FAILED = "Validation failed.";
    public static final String UNEXPECTED_ERROR = "Unexpected error occurred.";
    public static final String SESSION_FEEDBACK_REQUIRES_CONVERSATION = "conversationId is required for session feedback.";
    public static final String AI_STREAM_FAILURE = "AI streaming request failed.";
    public static final String AI_REQUEST_FAILURE = "AI service request failed.";
    public static final String AI_CIRCUIT_OPEN = "AI service is temporarily unavailable.";
    public static final String VERSES_STORE_FAILURE = "Failed to store verse references.";
    public static final String HIGH_RISK_SAFE_RESPONSE = "I’m sorry you’re carrying something this heavy. I’m not the right support for immediate crisis care. Please contact a trusted person now and reach out to a local crisis line or emergency service right away.";
    public static final String HIGH_RISK_REFLECTION = "Who can you contact immediately for real-world support in the next few minutes?";

    private MessageConstants() {
    }
}
