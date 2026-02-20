package com.integrador.api.orders.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @JsonAlias({"customerId", "userId"}) Long customerId,
        String salesChannel,
        String destination,
        @NotEmpty List<@Valid OrderItemRequest> items
) {
    public record OrderItemRequest(
            @JsonAlias({"productId", "id"}) Long productId,
            Integer quantity
    ) {}
}
