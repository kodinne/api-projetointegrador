package com.integrador.api.returns.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateReturnRequest(
        @NotNull Long orderId,
        @NotNull Long productId,
        @NotNull Integer quantity,
        String reason,
        BigDecimal value
) {
}
