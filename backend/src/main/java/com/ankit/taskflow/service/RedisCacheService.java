package com.ankit.taskflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> get(String key, Class<T> type) {
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (raw == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(raw, type));
        } catch (JsonProcessingException exception) {
            stringRedisTemplate.delete(key);
            return Optional.empty();
        }
    }

    public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (raw == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(raw, typeReference));
        } catch (JsonProcessingException exception) {
            stringRedisTemplate.delete(key);
            return Optional.empty();
        }
    }

    public void put(String key, Object value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize cache payload", exception);
        }
    }

    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public void deleteByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
