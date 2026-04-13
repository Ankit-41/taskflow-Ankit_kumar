package com.ankit.taskflow.idempotency;

import com.ankit.taskflow.exception.BadRequestException;
import com.ankit.taskflow.security.UserPrincipal;
import com.ankit.taskflow.service.RedisCacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String IDEMPOTENCY_ATTRIBUTE = "taskflow.idempotency.key";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final RedisCacheService redisCacheService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!requiresIdempotency(request)) {
            return true;
        }

        String headerValue = request.getHeader(IDEMPOTENCY_HEADER);
        if (!StringUtils.hasText(headerValue)) {
            throw new BadRequestException("validation failed", Map.of(IDEMPOTENCY_HEADER, "header is required"));
        }

        String cacheKey = buildCacheKey(request, headerValue);
        IdempotencyPayload payload = redisCacheService.get(cacheKey, IdempotencyPayload.class).orElse(null);
        if (payload != null) {
            response.setStatus(payload.status());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-Idempotent-Replay", "true");
            response.getWriter().write(payload.body());
            return false;
        }

        request.setAttribute(IDEMPOTENCY_ATTRIBUTE, cacheKey);
        return true;
    }

    private boolean requiresIdempotency(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();
        return "/projects".equals(path) || pathMatcher.match("/projects/*/tasks", path);
    }

    private String buildCacheKey(HttpServletRequest request, String headerValue) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userScope = "anonymous";
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            userScope = principal.getUserId().toString();
        }

        return "idempotency:%s:%s:%s:%s".formatted(
                userScope,
                request.getMethod(),
                request.getRequestURI(),
                headerValue.trim());
    }
}

