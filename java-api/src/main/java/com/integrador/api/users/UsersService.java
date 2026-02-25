package com.integrador.api.users;

import com.integrador.api.orders.Order;
import com.integrador.api.orders.OrderRepository;
import com.integrador.api.users.dto.CreateUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UsersService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UsersService(UserRepository userRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    public User create(CreateUserRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String password = request.password() == null ? "" : request.password();

        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (password.isBlank() || password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must have at least 6 characters");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(10)));
        return userRepository.save(user);
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public List<Map<String, Object>> findAllPublic() {
        List<Map<String, Object>> users = new ArrayList<>();
        userRepository.findAll().forEach(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", u.getId());
            row.put("name", u.getName());
            row.put("email", u.getEmail());
            users.add(row);
        });
        return users;
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        User user = findById(id);

        String name = asString(body.get("name"));
        String email = asString(body.get("email"));
        String phone = asString(body.get("phone"));
        String password = asString(body.get("password"));

        if (name != null && !name.isBlank()) {
            user.setName(name.trim());
        }

        if (phone != null) {
            user.setPhone(phone.trim());
        }

        if (email != null && !email.isBlank()) {
            String normalized = email.trim().toLowerCase();
            userRepository.findByEmail(normalized).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
                }
            });
            user.setEmail(normalized);
        }

        if (password != null && !password.isBlank()) {
            user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(10)));
        }

        User saved = userRepository.save(user);
        return toPublicMap(saved);
    }

    @Transactional
    public void delete(Long id) {
        User user = findById(id);

        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> o.getCustomer() != null && o.getCustomer().getId().equals(user.getId()))
                .toList();

        for (Order order : orders) {
            // Preserve display name for history and remove FK dependency.
            if (order.getCustomerName() == null || order.getCustomerName().isBlank()) {
                order.setCustomerName(user.getName());
            }
            order.setCustomer(null);
        }

        if (!orders.isEmpty()) {
            orderRepository.saveAll(orders);
        }

        userRepository.delete(user);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> toPublicMap(User u) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", u.getId());
        row.put("name", u.getName());
        row.put("email", u.getEmail());
        row.put("phone", u.getPhone());
        return row;
    }
}
