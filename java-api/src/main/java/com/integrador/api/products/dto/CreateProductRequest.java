package com.integrador.api.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        @DecimalMin(value = "0.00") BigDecimal costPrice,
        @NotNull @Min(1) Integer stock
) {}
