package com.integrador.api.orders;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    long deleteByProduct_Id(Long productId);
}
