package com.exercise.bookmateserver.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;

@Component
public class AppleIdentityTokenVerifier {

    private static final URI APPLE_KEYS_URI = URI.create("https://appleid.apple.com/auth/keys");
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final String APPLE_ALGORITHM = "RS256";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String clientId;

    public AppleIdentityTokenVerifier(
            ObjectMapper objectMapper,
            @Value("${app.apple.client-id:cozyrim.BookMate-mini}") String clientId
    ) {
        this.objectMapper = objectMapper;
        this.clientId = clientId;
    }

    public AppleUserInfo verify(String identityToken, String expectedUserIdentifier) {
        String[] parts = identityToken.split("\\.");
        if (parts.length != 3) {
            throw unauthorized("Apple 로그인 토큰 형식이 올바르지 않습니다.");
        }

        try {
            JsonNode header = decodeJson(parts[0]);
            JsonNode payload = decodeJson(parts[1]);

            validateClaims(payload, expectedUserIdentifier);
            verifySignature(parts, header);

            return new AppleUserInfo(
                    payload.path("sub").asText(),
                    payload.path("email").asText(null)
            );
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized("Apple 로그인 토큰을 확인할 수 없습니다.");
        }
    }

    private void validateClaims(JsonNode payload, String expectedUserIdentifier) {
        String issuer = payload.path("iss").asText(null);
        if (!APPLE_ISSUER.equals(issuer)) {
            throw unauthorized("Apple 로그인 발급자를 확인할 수 없습니다.");
        }

        if (!hasExpectedAudience(payload.path("aud"))) {
            throw unauthorized("Apple 로그인 대상 앱이 일치하지 않습니다.");
        }

        long expiration = payload.path("exp").asLong(0);
        if (expiration < Instant.now().getEpochSecond()) {
            throw unauthorized("Apple 로그인 토큰이 만료되었습니다.");
        }

        String subject = payload.path("sub").asText(null);
        if (subject == null || subject.isBlank()) {
            throw unauthorized("Apple 사용자 식별자를 확인할 수 없습니다.");
        }

        if (expectedUserIdentifier == null || expectedUserIdentifier.isBlank() || !subject.equals(expectedUserIdentifier)) {
            throw unauthorized("Apple 사용자 식별자가 일치하지 않습니다.");
        }
    }

    private boolean hasExpectedAudience(JsonNode audience) {
        if (audience.isTextual()) {
            return clientId.equals(audience.asText());
        }

        if (audience.isArray()) {
            for (JsonNode item : audience) {
                if (clientId.equals(item.asText())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void verifySignature(String[] parts, JsonNode header) throws Exception {
        String algorithm = header.path("alg").asText(null);
        String keyId = header.path("kid").asText(null);

        if (!APPLE_ALGORITHM.equals(algorithm) || keyId == null || keyId.isBlank()) {
            throw unauthorized("Apple 로그인 서명 정보를 확인할 수 없습니다.");
        }

        PublicKey publicKey = fetchPublicKey(keyId);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));

        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
        if (!signature.verify(signatureBytes)) {
            throw unauthorized("Apple 로그인 토큰 서명이 올바르지 않습니다.");
        }
    }

    private PublicKey fetchPublicKey(String keyId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(APPLE_KEYS_URI)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Apple 공개키를 가져올 수 없습니다.");
        }

        JsonNode keys = objectMapper.readTree(response.body()).path("keys");
        for (JsonNode key : keys) {
            if (keyId.equals(key.path("kid").asText())) {
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("n").asText()));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("e").asText()));
                RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                return KeyFactory.getInstance("RSA").generatePublic(spec);
            }
        }

        throw unauthorized("Apple 로그인 공개키를 찾을 수 없습니다.");
    }

    private JsonNode decodeJson(String encoded) throws IOException {
        byte[] bytes = Base64.getUrlDecoder().decode(encoded);
        return objectMapper.readTree(bytes);
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
