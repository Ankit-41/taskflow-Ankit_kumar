package com.ankit.taskflow.security;

import com.ankit.taskflow.config.TaskFlowProperties;
import com.ankit.taskflow.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationHours;

    public JwtService(TaskFlowProperties properties) {
        this.signingKey = buildKey(properties.getSecurity().getJwtSecret());
        this.expirationHours = properties.getSecurity().getJwtExpirationHours();
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .subject(user.getEmail())
                .claims(Map.of(
                        "user_id", user.getId().toString(),
                        "email", user.getEmail()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).get("user_id", String.class));
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public boolean isTokenValid(String token, UserPrincipal principal) {
        Claims claims = parseClaims(token);
        return principal.getUsername().equalsIgnoreCase(claims.getSubject())
                && claims.getExpiration().toInstant().isAfter(Instant.now());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey buildKey(String rawSecret) {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(rawSecret));
        } catch (IllegalArgumentException ignored) {
            return Keys.hmacShaKeyFor(rawSecret.getBytes(StandardCharsets.UTF_8));
        }
    }
}

