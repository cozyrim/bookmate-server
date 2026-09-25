package com.exercise.bookmateserver.ratelimit;

import com.exercise.bookmateserver.auth.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserWriteLimitInterceptor implements HandlerInterceptor {
    private final UserWriteLimitService limits;
    private final CurrentUserResolver users;

    public UserWriteLimitInterceptor(UserWriteLimitService limits, CurrentUserResolver users) {
        this.limits = limits;
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method) || !"POST".equals(request.getMethod())) return true;
        LimitedWrite rule = method.getMethodAnnotation(LimitedWrite.class);
        if (rule == null) return true;
        long retryAfter = limits.consume(users.get(request).getId(), rule.value());
        if (retryAfter > 0) {
            response.setHeader("Retry-After", Long.toString(retryAfter));
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "요청이 많습니다. 잠시 후 다시 시도해주세요.");
        }
        return true;
    }
}
