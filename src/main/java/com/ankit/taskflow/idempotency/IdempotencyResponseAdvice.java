package com.ankit.taskflow.idempotency;

import com.ankit.taskflow.config.TaskFlowProperties;
import com.ankit.taskflow.service.RedisCacheService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations = RestController.class)
@RequiredArgsConstructor
public class IdempotencyResponseAdvice implements ResponseBodyAdvice<Object> {

    private final RedisCacheService redisCacheService;
    private final ObjectMapper objectMapper;
    private final TaskFlowProperties properties;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)
                || !(response instanceof ServletServerHttpResponse servletResponse)) {
            return body;
        }

        Object key = servletRequest.getServletRequest().getAttribute(IdempotencyInterceptor.IDEMPOTENCY_ATTRIBUTE);
        if (!(key instanceof String cacheKey)) {
            return body;
        }

        int status = servletResponse.getServletResponse().getStatus();
        if (status < 200 || status >= 300) {
            return body;
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            redisCacheService.put(
                    cacheKey,
                    new IdempotencyPayload(status, jsonBody),
                    properties.getCache().getIdempotencyTtl());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to cache idempotent response", exception);
        }

        return body;
    }
}

