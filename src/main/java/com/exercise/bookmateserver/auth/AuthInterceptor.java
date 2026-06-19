package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final TokenService tokenService;
    private final UserRepository userRepository;

    public AuthInterceptor(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!requiresAuthentication(request)) {
            return true;
        }

        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String accessToken = authorizationHeader.substring("Bearer ".length()).trim();
        UUID userId = tokenService.parseUserId(accessToken);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));

        request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
        return true;
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();

        return path.equals("/api/me")
                || path.startsWith("/api/me/")
                || path.equals("/api/users/me")
                || path.equals("/api/books")
                || path.startsWith("/api/books/")
                || path.equals("/api/words")
                || path.startsWith("/api/words/")
                || path.equals("/api/quotes")
                || path.startsWith("/api/quotes/")
                || path.equals("/api/reviews")
                || path.startsWith("/api/reviews/")
                || path.equals("/api/reading-memos")
                || path.startsWith("/api/reading-memos/")
                || path.equals("/api/moderation")
                || path.startsWith("/api/moderation/")
                || path.equals("/api/social")
                || path.startsWith("/api/social/");
    }
}
