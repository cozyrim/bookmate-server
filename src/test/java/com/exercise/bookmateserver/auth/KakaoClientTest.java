package com.exercise.bookmateserver.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class KakaoClientTest {
    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(String json) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(json);
        return response;
    }

    @Test
    void rejectsTokenForAnotherAppBeforeFetchingUser() throws Exception {
        HttpClient http = mock(HttpClient.class);
        var tokenResponse = response("{\"app_id\":999,\"id\":123,\"expires_in\":60}");
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(tokenResponse);
        KakaoClient client = new KakaoClient(new ObjectMapper(), "100");
        ReflectionTestUtils.setField(client, "httpClient", http);
        assertThatThrownBy(() -> client.fetchUserInfo("test-token"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(401));
        verify(http, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    void acceptsMatchingAppAndUser() throws Exception {
        HttpClient http = mock(HttpClient.class);
        var tokenResponse = response("{\"app_id\":100,\"id\":123,\"expires_in\":60}");
        var userResponse = response("{\"id\":123,\"kakao_account\":{}}");
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(tokenResponse, userResponse);
        KakaoClient client = new KakaoClient(new ObjectMapper(), "100");
        ReflectionTestUtils.setField(client, "httpClient", http);
        assertThat(client.fetchUserInfo("test-token").providerId()).isEqualTo("123");
    }

    @Test
    void missingAppConfigurationFailsClosed() {
        KakaoClient client = new KakaoClient(new ObjectMapper(), "");
        assertThatThrownBy(() -> client.fetchUserInfo("test-token"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(503));
    }
}
