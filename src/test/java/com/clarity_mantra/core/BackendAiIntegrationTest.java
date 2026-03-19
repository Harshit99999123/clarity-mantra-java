package com.clarity_mantra.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BackendAiIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Test
    void backendCanCreateConversationAndCallLiveAiService() throws Exception {
        String token = registerUserAndGetToken();
        long conversationId = createConversation(token);
        JsonNode messageResponse = postMessage(token, conversationId);

        assertThat(messageResponse.path("success").asBoolean()).isTrue();
        assertThat(messageResponse.path("data").path("content").asText()).isNotBlank();
    }

    private String registerUserAndGetToken() throws Exception {
        String requestBody = """
                {
                  "fullName": "Integration User",
                  "email": "%s",
                  "password": "StrongPass123",
                  "preferredLanguage": "en"
                }
                """.formatted("integration.user+" + System.nanoTime() + "@example.com");

        HttpResponse<String> response = send("/auth/register", "POST", requestBody, null);
        JsonNode json = objectMapper.readTree(response.body());
        return json.path("data").path("accessToken").asText();
    }

    private long createConversation(String token) throws Exception {
        String requestBody = """
                {
                  "title": "Integration Reflection",
                  "languageCode": "en"
                }
                """;

        HttpResponse<String> response = send("/conversations", "POST", requestBody, token);
        JsonNode json = objectMapper.readTree(response.body());
        return json.path("data").path("id").asLong();
    }

    private JsonNode postMessage(String token, long conversationId) throws Exception {
        String requestBody = """
                {
                  "message": "I feel confused about my career path and want grounded guidance.",
                  "inputMode": "TEXT"
                }
                """;

        HttpResponse<String> response = send("/conversations/" + conversationId + "/messages", "POST", requestBody, token);
        return objectMapper.readTree(response.body());
    }

    private HttpResponse<String> send(String path, String method, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(120))
                .header("Content-Type", "application/json");

        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }

        HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.ofString(body)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .withFailMessage("Expected 2xx response but got %s with body: %s", response.statusCode(), response.body())
                .isBetween(200, 299);
        return response;
    }
}
