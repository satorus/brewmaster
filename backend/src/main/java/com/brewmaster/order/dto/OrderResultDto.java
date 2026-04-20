package com.brewmaster.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderResultDto(
        UUID orderId,
        UUID recipeId,
        String recipeName,
        BigDecimal volumeL,
        List<OrderItemDto> items,
        double estimatedTotalMin,
        double estimatedTotalMax,
        String generatedAt,
        String disclaimer
) {}
