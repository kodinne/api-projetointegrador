package com.integrador.api.dashboard;

import com.integrador.api.orders.Order;
import com.integrador.api.orders.OrderRepository;
import com.integrador.api.products.Product;
import com.integrador.api.products.ProductRepository;
import com.integrador.api.returns.ReturnRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReturnRecordRepository returnRecordRepository;

    public DashboardController(OrderRepository orderRepository, ProductRepository productRepository, ReturnRecordRepository returnRecordRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.returnRecordRepository = returnRecordRepository;
    }

    @GetMapping
    public ResponseEntity<?> metrics(@RequestParam(required = false) String period) {
        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> inPeriod(o.getCreatedAt(), period))
                .filter(o -> !"returned".equalsIgnoreCase(o.getStatus()))
                .toList();

        BigDecimal revenue = orders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal salesReturn = returnRecordRepository.findAll().stream()
                .filter(r -> inPeriod(r.getDate(), period))
                .map(r -> r.getValue() == null ? BigDecimal.ZERO : r.getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchase = BigDecimal.ZERO;
        BigDecimal income = revenue.subtract(purchase);

        Map<String, Integer> topSelling = new HashMap<>();
        for (Order order : orders) {
            order.getItems().forEach(item -> {
                if (item.getProduct() != null) {
                    topSelling.merge(item.getProduct().getName(), item.getQuantity(), Integer::sum);
                }
            });
        }

        List<Map<String, Object>> topSellingArray = topSelling.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .<Map<String, Object>>map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", entry.getKey());
                    row.put("qty", entry.getValue());
                    return row;
                })
                .toList();

        List<Product> stockAlert = productRepository.findAll().stream().filter(p -> p.getStock() <= 5).toList();

        return ResponseEntity.ok(Map.of(
                "cards", Map.of("revenue", revenue, "salesReturn", salesReturn, "purchase", purchase, "income", income),
                "topSelling", topSellingArray,
                "stockAlert", stockAlert
        ));
    }

    private boolean inPeriod(LocalDateTime value, String period) {
        if (value == null || period == null || period.isBlank() || "all".equalsIgnoreCase(period)) return true;
        LocalDate today = LocalDate.now();
        return switch (period.toLowerCase()) {
            case "today" -> value.toLocalDate().isEqual(today);
            case "7d" -> !value.toLocalDate().isBefore(today.minusDays(6));
            case "30d" -> !value.toLocalDate().isBefore(today.minusDays(29));
            default -> true;
        };
    }
}
