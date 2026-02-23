package com.integrador.api.config;

import com.integrador.api.auth.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class ApiAuthInterceptor implements HandlerInterceptor {
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/auth/login"
    );
    private final JwtService jwtService;

    public ApiAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String path = request.getRequestURI();
        if (isPublic(request.getMethod(), path)) return true;

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, "Token ausente.");
            return false;
        }

        String token = authHeader.substring(7).trim();
        if (token.isBlank() || !jwtService.isTokenValid(token)) {
            writeUnauthorized(response, "Token invalido ou expirado.");
            return false;
        }

        return true;
    }

    private boolean isPublic(String method, String path) {
        if (PUBLIC_PATHS.contains(path)) return true;
        return "POST".equalsIgnoreCase(method) && "/users".equals(path);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
        response.getWriter().flush();
    }
}
