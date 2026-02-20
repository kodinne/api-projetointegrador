package com.integrador.api.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String category,
        @NotNull BigDecimal price,
        @NotNull Integer stock
) {}
