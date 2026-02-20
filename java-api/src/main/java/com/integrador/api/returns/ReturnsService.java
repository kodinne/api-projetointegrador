package com.integrador.api.returns;

import com.integrador.api.orders.Order;
import com.integrador.api.orders.OrderItem;
import com.integrador.api.orders.OrderRepository;
import com.integrador.api.products.Product;
import com.integrador.api.products.ProductRepository;
import com.integrador.api.returns.dto.CreateReturnRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class ReturnsService {
    private final ReturnRecordRepository returnRecordRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public ReturnsService(ReturnRecordRepository returnRecordRepository, OrderRepository orderRepository, ProductRepository productRepository) {
        this.returnRecordRepository = returnRecordRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Map<String, Object> create(CreateReturnRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be greater than zero");
        }

        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getProduct() != null && request.productId().equals(i.getProduct().getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product not found in this order"));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        int quantity = request.quantity();
        if (quantity > item.getQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Return quantity exceeds ordered quantity");
        }

        // devolve itens ao estoque
        product.setStock(product.getStock() + quantity);
        productRepository.save(product);

        // reduz item do pedido
        item.setQuantity(item.getQuantity() - quantity);
        order.getItems().removeIf(i -> i.getQuantity() <= 0);

        // pedido com devolucao deve sair da tela de pedidos
        order.setStatus("returned");
        orderRepository.save(order);

        BigDecimal unitPrice = item.getProduct() != null && item.getProduct().getPrice() != null
                ? item.getProduct().getPrice()
                : BigDecimal.ZERO;
        BigDecimal value = unitPrice.multiply(BigDecimal.valueOf(quantity));

        ReturnRecord record = new ReturnRecord();
        record.setOrderId(order.getId());
        record.setProductId(product.getId());
        record.setQuantity(quantity);
        record.setReason((request.reason() == null || request.reason().isBlank()) ? "Sem motivo informado" : request.reason());
        record.setValue(value);
        ReturnRecord saved = returnRecordRepository.save(record);

        return Map.of(
                "id", saved.getId(),
                "orderId", saved.getOrderId(),
                "productId", saved.getProductId(),
                "quantity", saved.getQuantity(),
                "reason", saved.getReason(),
                "value", saved.getValue(),
                "date", saved.getDate()
        );
    }

    public Map<String, Object> list() {
        var items = returnRecordRepository.findAll();
        return Map.of("items", items, "total", items.size());
    }
}
