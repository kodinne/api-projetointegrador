package com.integrador.api.auth;

import com.integrador.api.users.User;
import com.integrador.api.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public Map<String, Object> login(String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (password == null || password.isBlank() || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getName());
        Map<String, Object> payload = Map.of("sub", user.getId(), "email", user.getEmail(), "name", user.getName());

        // Keep both keys for backward compatibility with existing frontend clients.
        return Map.of("access_token", token, "token", token, "user", payload);
    }
}
