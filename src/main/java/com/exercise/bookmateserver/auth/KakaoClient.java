package com.exercise.bookmateserver.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class KakaoClient {

    private static final URI KAKAO_USER_INFO_URI = URI.create("https://kapi.kakao.com/v2/user/me?secure_resource=true");

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String appId;
    private final ObjectMapper objectMapper;

    public KakaoClient(ObjectMapper objectMapper, @Value("${app.kakao.app-id:}") String appId) {
        this.objectMapper = objectMapper;
        this.appId = appId.trim();
    }

    public KakaoUserInfo fetchUserInfo(String accessToken) {
        if (appId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "카카오 로그인 설정을 확인해주세요.");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 로그인 정보가 필요합니다.");
        }
        HttpRequest request = HttpRequest.newBuilder(KAKAO_USER_INFO_URI)
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        try {
            HttpRequest tokenRequest = HttpRequest.newBuilder(URI.create("https://kapi.kakao.com/v1/user/access_token_info"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET().build();
            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            if (tokenResponse.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 토큰을 확인할 수 없습니다.");
            }
            JsonNode tokenInfo = objectMapper.readTree(tokenResponse.body());
            if (!appId.equals(tokenInfo.path("app_id").asText()) || tokenInfo.path("expires_in").asLong() <= 0) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 로그인 대상 앱 또는 유효기간이 일치하지 않습니다.");
            }
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 로그인 정보 확인에 실패했습니다.");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String providerId = root.path("id").asText(null);

            if (providerId == null || providerId.isBlank() || !providerId.equals(tokenInfo.path("id").asText())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "카카오 사용자 ID를 확인할 수 없습니다.");
            }

            JsonNode kakaoAccount = root.path("kakao_account");
            JsonNode profile = kakaoAccount.path("profile");

            String email = kakaoAccount.path("email").asText(null);
            String nickname = profile.path("nickname").asText("북메이트");
            String profileImageUrl = profile.path("profile_image_url").asText(null);

            return new KakaoUserInfo(providerId, email, nickname, profileImageUrl);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 서버 응답을 읽을 수 없습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 서버 요청이 중단되었습니다.");
        }
    }
}
