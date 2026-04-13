package com.ankit.taskflow.service;

import com.ankit.taskflow.dto.auth.AuthResponse;
import com.ankit.taskflow.dto.auth.LoginRequest;
import com.ankit.taskflow.dto.auth.RegisterRequest;
import com.ankit.taskflow.exception.ConflictException;
import com.ankit.taskflow.exception.UnauthorizedException;
import com.ankit.taskflow.model.User;
import com.ankit.taskflow.repository.UserRepository;
import com.ankit.taskflow.security.JwtService;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .id(UUID.randomUUID())
                .name(request.name().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.extractExpiration(token), user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.extractExpiration(token), user.getId(), user.getEmail());
    }
}
