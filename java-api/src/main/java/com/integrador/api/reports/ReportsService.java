package com.integrador.api.reports;

import com.integrador.api.orders.Order;
import com.integrador.api.orders.OrderRepository;
import com.integrador.api.products.Product;
import com.integrador.api.products.ProductRepository;
import com.integrador.api.returns.ReturnRecord;
import com.integrador.api.returns.ReturnRecordRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReportsService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReturnRecordRepository returnRecordRepository;

    public ReportsService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            ReturnRecordRepository returnRecordRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.returnRecordRepository = returnRecordRepository;
    }

    public Map<String, Object> summary(String period, LocalDate startDate, LocalDate endDate) {
        String resolvedPeriod = period == null || period.isBlank() ? "all" : period;

        List<Order> orders = orderRepository.findAll().stream()
                .filter(o -> inPeriod(o.getCreatedAt(), resolvedPeriod, startDate, endDate))
                .toList();

        List<Order> completedOrders = orders.stream()
                .filter(o -> "completed".equalsIgnoreCase(o.getStatus()))
                .toList();

        List<ReturnRecord> returns = returnRecordRepository.findAll().stream()
                .filter(r -> inPeriod(r.getDate(), resolvedPeriod, startDate, endDate))
                .toList();

        BigDecimal grossSales = completedOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> {
                    if (i.getProduct() == null || i.getProduct().getPrice() == null) return BigDecimal.ZERO;
                    return i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal returnsValue = returns.stream()
                .map(r -> r.getValue() == null ? BigDecimal.ZERO : r.getValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSales = grossSales.subtract(returnsValue);

        BigDecimal margin = completedOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(i -> {
                    if (i.getProduct() == null || i.getProduct().getPrice() == null) return BigDecimal.ZERO;
                    BigDecimal cost = i.getProduct().getCostPrice() == null ? BigDecimal.ZERO : i.getProduct().getCostPrice();
                    BigDecimal unitMargin = i.getProduct().getPrice().subtract(cost);
                    return unitMargin.multiply(BigDecimal.valueOf(i.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal marginRate = netSales.signum() == 0
                ? BigDecimal.ZERO
                : margin.divide(netSales, 4, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        Set<Long> soldProductIds = new HashSet<>();
        completedOrders.forEach(order ->
                order.getItems().forEach(item -> {
                    if (item.getProduct() != null && item.getProduct().getId() != null) {
                        soldProductIds.add(item.getProduct().getId());
                    }
                })
        );

        List<Map<String, Object>> topSelling = new ArrayList<>();
        Map<String, Integer> salesByProduct = new HashMap<>();
        completedOrders.forEach(order ->
                order.getItems().forEach(item -> {
                    if (item.getProduct() != null) {
                        salesByProduct.merge(item.getProduct().getName(), item.getQuantity(), Integer::sum);
                    }
                })
        );
        salesByProduct.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .forEach(entry -> topSelling.add(Map.of("name", entry.getKey(), "qty", entry.getValue())));

        List<Map<String, Object>> lowStock = productRepository.findAll().stream()
                .filter(p -> p.getStock() != null && p.getStock() <= 5)
                .sorted(Comparator.comparing(Product::getStock))
                .<Map<String, Object>>map(p -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", p.getId());
                    row.put("name", p.getName());
                    row.put("sku", p.getSku());
                    row.put("stock", p.getStock());
                    return row;
                })
                .toList();

        List<Map<String, Object>> stalledProducts = productRepository.findAll().stream()
                .filter(p -> p.getStatus() != null && "active".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getStock() != null && p.getStock() > 0)
                .filter(p -> p.getId() != null && !soldProductIds.contains(p.getId()))
                .<Map<String, Object>>map(p -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", p.getId());
                    row.put("name", p.getName());
                    row.put("sku", p.getSku());
                    row.put("stock", p.getStock());
                    row.put("updatedAt", p.getUpdatedAt());
                    return row;
                })
                .toList();

        List<Map<String, Object>> returnsSummary = returns.stream()
                .sorted(Comparator.comparing(ReturnRecord::getDate).reversed())
                .limit(20)
                .<Map<String, Object>>map(r -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", r.getId());
                    row.put("orderId", r.getOrderId());
                    row.put("productId", r.getProductId());
                    row.put("quantity", r.getQuantity());
                    row.put("value", r.getValue());
                    row.put("date", r.getDate());
                    row.put("reason", r.getReason());
                    return row;
                })
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("period", resolvedPeriod);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("cards", Map.of(
                "grossSales", grossSales,
                "returns", returnsValue,
                "netSales", netSales,
                "margin", margin,
                "marginRate", marginRate
        ));
        response.put("lowStock", lowStock);
        response.put("stalledProducts", stalledProducts);
        response.put("topSelling", topSelling);
        response.put("returns", returnsSummary);

        return response;
    }

    private boolean inPeriod(LocalDateTime value, String period, LocalDate startDate, LocalDate endDate) {
        if (value == null) return true;

        LocalDate date = value.toLocalDate();
        if (startDate != null || endDate != null) {
            if (startDate != null && date.isBefore(startDate)) return false;
            if (endDate != null && date.isAfter(endDate)) return false;
            return true;
        }

        if (value == null || period == null || period.isBlank() || "all".equalsIgnoreCase(period)) return true;
        LocalDate today = LocalDate.now();
        return switch (period.toLowerCase()) {
            case "today" -> date.isEqual(today);
            case "7d" -> !date.isBefore(today.minusDays(6));
            case "30d" -> !date.isBefore(today.minusDays(29));
            default -> true;
        };
    }
}
