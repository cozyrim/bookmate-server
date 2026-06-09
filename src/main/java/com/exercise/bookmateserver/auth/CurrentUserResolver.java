package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserResolver {

    public UserEntity get(HttpServletRequest request) {
        Object currentUser = request.getAttribute(AuthInterceptor.CURRENT_USER_ATTRIBUTE);

        if (currentUser instanceof UserEntity user) {
            return user;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
