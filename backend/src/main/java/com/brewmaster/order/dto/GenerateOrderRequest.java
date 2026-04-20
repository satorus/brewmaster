package com.brewmaster.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record GenerateOrderRequest(
        @NotNull UUID recipeId,
        @NotNull @DecimalMin("1") BigDecimal volumeL
) {}
