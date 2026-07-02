package com.exercise.bookmateserver.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class DiscordLoginNotificationService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String webhookUrl;

    public DiscordLoginNotificationService(
            ObjectMapper objectMapper,
            @Value("${app.discord.login-webhook-url:}") String webhookUrl
    ) {
        this.objectMapper = objectMapper;
        this.webhookUrl = webhookUrl;
    }

    public void notifySignup(String method, SignupStats stats) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            Map<String, Object> embed = Map.of(
                    "title", "BookMate 신규 가입",
                    "color", 5814783,
                    "fields", List.of(
                            Map.of(
                                    "name", "가입 경로",
                                    "value", "**" + displayMethod(method) + "**",
                                    "inline", true
                            ),
                            Map.of(
                                    "name", "총 가입 수",
                                    "value", "**" + stats.total() + "명**",
                                    "inline", true
                            ),
                            Map.of(
                                    "name", "경로별 가입 수",
                                    "value", """
                                            BookMate: %d명
                                            Kakao: %d명
                                            Apple: %d명
                                            """.formatted(stats.bookmate(), stats.kakao(), stats.apple()),
                                    "inline", false
                            )
                    ),
                    "timestamp", Instant.now().toString()
            );

            Map<String, Object> payload = Map.of(
                    "embeds", List.of(embed),
                    "allowed_mentions", Map.of("parse", List.of())
            );

            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Login should not fail just because an admin notification failed.
        }
    }

    private String displayMethod(String method) {
        return switch (method) {
            case "apple" -> "Apple";
            case "kakao" -> "Kakao";
            case "bookmate" -> "BookMate";
            default -> method;
        };
    }
}
