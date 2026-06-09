package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final byte[] secretBytes;
    private final long expirationSeconds;

    public TokenService(
            ObjectMapper objectMapper,
            @Value("${auth.jwt.secret:bookmate-local-development-secret-change-me}") String secret,
            @Value("${auth.jwt.expiration-seconds:1209600}") long expirationSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String createAccessToken(UserEntity user) {
        long now = Instant.now().getEpochSecond();

        Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
        );
        Map<String, Object> payload = Map.of(
                "sub", user.getId().toString(),
                "iat", now,
                "exp", now + expirationSeconds
        );

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String unsignedToken = encodedHeader + "." + encodedPayload;
        String signature = sign(unsignedToken);

        return unsignedToken + "." + signature;
    }

    public UUID parseUserId(String token) {
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "올바르지 않은 토큰입니다.");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);

        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 서명이 올바르지 않습니다.");
        }

        try {
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });

            Object exp = payload.get("exp");
            if (!(exp instanceof Number expNumber) || expNumber.longValue() < Instant.now().getEpochSecond()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            }

            Object sub = payload.get("sub");
            if (!(sub instanceof String userId)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 사용자 정보를 확인할 수 없습니다.");
            }

            return UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰 사용자 정보가 올바르지 않습니다.");
        } catch (Exception exception) {
            if (exception instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }

            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰을 해석할 수 없습니다.");
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception exception) {
            throw new IllegalStateException("토큰 JSON 생성에 실패했습니다.", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("토큰 서명 생성에 실패했습니다.", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);

        if (expectedBytes.length != actualBytes.length) {
            return false;
        }

        int result = 0;
        for (int index = 0; index < expectedBytes.length; index++) {
            result |= expectedBytes[index] ^ actualBytes[index];
        }

        return result == 0;
    }
}
