package com.integrador.api.orders;

import com.integrador.api.orders.dto.CreateOrderRequest;
import com.integrador.api.products.Product;
import com.integrador.api.products.ProductRepository;
import com.integrador.api.returns.ReturnRecordRepository;
import com.integrador.api.users.User;
import com.integrador.api.users.UserRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrdersService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReturnRecordRepository returnRecordRepository;

    public OrdersService(
            OrderRepository orderRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ReturnRecordRepository returnRecordRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.returnRecordRepository = returnRecordRepository;
    }

    @Transactional
    public Order create(CreateOrderRequest request) {
        User customer = resolveCustomer(request.customerId());
        String customCustomerName = request.customerName() == null ? "" : request.customerName().trim();

        Order order = new Order();
        order.setCustomer(customer);
        order.setCustomerName(
                !customCustomerName.isBlank()
                        ? customCustomerName
                        : (customer != null ? customer.getName() : "Cliente")
        );
        order.setSalesChannel(request.salesChannel() == null ? "sales" : request.salesChannel());
        order.setDestination(request.destination() == null ? "warehouse" : request.destination());
        order.setStatus("completed");

        for (CreateOrderRequest.OrderItemRequest itemReq : request.items()) {
            if (itemReq.productId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required");
            }
            Product product = productRepository.findById(itemReq.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
            int quantity = itemReq.quantity() == null ? 1 : Math.max(1, itemReq.quantity());
            if (product.getStock() < quantity) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for product: " + product.getName());
            }

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(quantity);
            order.getItems().add(item);

            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
        }

        return orderRepository.save(order);
    }

    private User resolveCustomer(Long requestedCustomerId) {
        if (requestedCustomerId != null) {
            User byId = userRepository.findById(requestedCustomerId).orElse(null);
            if (byId != null) return byId;
        }

        User existingDefault = userRepository.findByEmail("pdv@local").orElse(null);
        if (existingDefault != null) return existingDefault;

        User firstUser = userRepository.findAll().stream().findFirst().orElse(null);
        if (firstUser != null) return firstUser;

        User fallback = new User();
        fallback.setName("Cliente PDV");
        fallback.setEmail("pdv@local");
        fallback.setPasswordHash(BCrypt.hashpw("pdv123456", BCrypt.gensalt(10)));
        return userRepository.save(fallback);
    }

    public Map<String, Object> findAll(int page, int limit, String status, String q) {
        int safePage = Math.max(1, page);
        int safeLimit = Math.max(1, Math.min(limit, 100));

        Specification<Order> spec = (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("customer", JoinType.LEFT);
                root.fetch("items", JoinType.LEFT).fetch("product", JoinType.LEFT);
                query.distinct(true);
            }

            var predicates = new ArrayList<Predicate>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.notEqual(root.get("status"), "returned"));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q + "%";
                predicates.add(cb.or(
                        cb.like(root.get("destination"), like),
                        cb.like(root.get("customer").get("name"), like),
                        cb.like(cb.function("str", String.class, root.get("id")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        var result = orderRepository.findAll(
                spec,
                PageRequest.of(safePage - 1, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<Map<String, Object>> mappedItems = result.getContent().stream().map(this::toOrderResponse).toList();
        return Map.of(
                "items", mappedItems,
                "total", result.getTotalElements(),
                "page", safePage,
                "limit", safeLimit
        );
    }

    public Map<String, Object> toOrderResponse(Order order) {
        List<Map<String, Object>> items = order.getItems().stream().map(i -> {
            Map<String, Object> row = new HashMap<>();
            row.put("productId", i.getProduct() != null ? i.getProduct().getId() : null);
            row.put("productName", i.getProduct() != null ? i.getProduct().getName() : null);
            row.put("quantity", i.getQuantity());
            row.put("unitPrice", i.getProduct() != null ? i.getProduct().getPrice() : null);
            if (i.getProduct() != null && i.getProduct().getPrice() != null) {
                row.put("subtotal", i.getProduct().getPrice().multiply(java.math.BigDecimal.valueOf(i.getQuantity())));
            } else {
                row.put("subtotal", java.math.BigDecimal.ZERO);
            }
            return row;
        }).toList();

        java.math.BigDecimal total = items.stream()
                .map(i -> (java.math.BigDecimal) i.get("subtotal"))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Map<String, Object> out = new HashMap<>();
        out.put("id", order.getId());
        out.put("customerId", order.getCustomer() != null ? order.getCustomer().getId() : null);
        out.put(
                "customerName",
                (order.getCustomerName() != null && !order.getCustomerName().isBlank())
                        ? order.getCustomerName()
                        : (order.getCustomer() != null ? order.getCustomer().getName() : "Cliente")
        );
        out.put("createdAt", order.getCreatedAt());
        out.put("date", order.getCreatedAt());
        out.put("salesChannel", order.getSalesChannel());
        out.put("destination", order.getDestination());
        out.put("status", order.getStatus());
        out.put("total", total);
        out.put("items", items);
        return out;
    }

    @Transactional
    public Map<String, Object> deleteAllSales() {
        List<Order> allOrders = orderRepository.findAll();
        if (allOrders.isEmpty()) {
            return Map.of(
                    "message", "Nenhuma venda para apagar",
                    "deletedOrders", 0
            );
        }

        for (Order order : allOrders) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
                    int currentStock = product.getStock() == null ? 0 : product.getStock();
                    product.setStock(currentStock + quantity);
                    productRepository.save(product);
                }
            }
        }

        long deletedCount = allOrders.size();
        long deletedReturns = returnRecordRepository.count();
        returnRecordRepository.deleteAll();
        orderRepository.deleteAll(allOrders);

        return Map.of(
                "message", "Vendas e devolucoes apagadas com sucesso",
                "deletedOrders", deletedCount,
                "deletedReturns", deletedReturns
        );
    }
}
