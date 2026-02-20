package com.integrador.api.config;

import com.integrador.api.orders.Order;
import com.integrador.api.orders.OrderItem;
import com.integrador.api.orders.OrderRepository;
import com.integrador.api.products.Product;
import com.integrador.api.products.ProductRepository;
import com.integrador.api.users.User;
import com.integrador.api.users.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataSeeder {
    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
    CommandLineRunner seed(UserRepository userRepository, ProductRepository productRepository, OrderRepository orderRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User user = new User();
                user.setName("Admin");
                user.setEmail("admin@example.com");
                user.setPasswordHash(BCrypt.hashpw("admin123", BCrypt.gensalt(10)));
                userRepository.save(user);
            }

            if (productRepository.count() == 0) {
                List<Product> samples = List.of(
                        makeProduct("INV-001", "Inverter", "cat1", new BigDecimal("1200.00"), 82),
                        makeProduct("BAT-002", "Battery", "cat2", new BigDecimal("300.00"), 5),
                        makeProduct("GEN-003", "Generator", "cat3", new BigDecimal("2500.00"), 60),
                        makeProduct("CHR-004", "Charger", "cat3", new BigDecimal("150.00"), 12),
                        makeProduct("PWR-005", "Power", "cat4", new BigDecimal("99.00"), 2)
                );
                productRepository.saveAll(samples);
            }

            if (orderRepository.count() == 0) {
                User admin = userRepository.findByEmail("admin@example.com").orElse(null);
                List<Product> products = productRepository.findAll();
                if (admin != null && products.size() > 1) {
                    Order order = new Order();
                    order.setCustomer(admin);
                    order.setSalesChannel("online");
                    order.setDestination("warehouse");
                    order.setStatus("completed");

                    OrderItem item1 = new OrderItem();
                    item1.setOrder(order);
                    item1.setProduct(products.get(0));
                    item1.setQuantity(2);

                    OrderItem item2 = new OrderItem();
                    item2.setOrder(order);
                    item2.setProduct(products.get(1));
                    item2.setQuantity(1);

                    order.getItems().add(item1);
                    order.getItems().add(item2);
                    orderRepository.save(order);
                }
            }
        };
    }

    private Product makeProduct(String sku, String name, String category, BigDecimal price, Integer stock) {
        Product product = new Product();
        product.setSku(sku);
        product.setName(name);
        product.setCategory(category);
        product.setPrice(price);
        product.setStock(stock);
        product.setStatus("active");
        return product;
    }
}
