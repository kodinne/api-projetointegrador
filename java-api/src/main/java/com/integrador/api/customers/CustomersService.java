package com.integrador.api.customers;

import com.integrador.api.users.User;
import com.integrador.api.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class CustomersService {
    private final UserRepository userRepository;

    public CustomersService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> list(String q) {
        String query = q == null ? "" : q.trim().toLowerCase();
        return userRepository.findAll().stream()
                .filter(u -> !"admin@example.com".equalsIgnoreCase(u.getEmail()))
                .filter(u -> query.isBlank()
                        || (u.getName() != null && u.getName().toLowerCase().contains(query))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(query))
                        || (u.getPhone() != null && u.getPhone().toLowerCase().contains(query)))
                .map(this::toMap)
                .toList();
    }

    public Map<String, Object> create(Map<String, Object> body) {
        String name = body == null ? null : asString(body.get("name"));
        String phone = body == null ? null : asString(body.get("phone"));
        String email = body == null ? null : asString(body.get("email"));

        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        String finalEmail = (email == null || email.isBlank())
                ? "cliente" + System.currentTimeMillis() + "@local"
                : email.trim().toLowerCase();
        if (userRepository.findByEmail(finalEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
        }

        User user = new User();
        user.setName(name.trim());
        user.setPhone(phone == null ? null : phone.trim());
        user.setEmail(finalEmail);
        user.setPasswordHash(BCrypt.hashpw("cliente123", BCrypt.gensalt(10)));
        return toMap(userRepository.save(user));
    }

    public Map<String, Object> update(Long id, Map<String, Object> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found"));

        String name = body == null ? null : asString(body.get("name"));
        String phone = body == null ? null : asString(body.get("phone"));
        String email = body == null ? null : asString(body.get("email"));

        if (name != null && !name.isBlank()) user.setName(name.trim());
        if (phone != null) user.setPhone(phone.trim());
        if (email != null && !email.isBlank()) {
            String normalized = email.trim().toLowerCase();
            userRepository.findByEmail(normalized).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
                }
            });
            user.setEmail(normalized);
        }

        return toMap(userRepository.save(user));
    }

    public void delete(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "customer not found"));
        userRepository.delete(user);
    }

    private String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Map<String, Object> toMap(User u) {
        return Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "phone", u.getPhone() == null ? "" : u.getPhone()
        );
    }
}
