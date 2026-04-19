package com.brewmaster.ai.dto;

import java.math.BigDecimal;

public record AiIngredientDto(
        String name,
        String category,
        BigDecimal amount,
        String unit,
        String additionTime,
        String notes,
        int sortOrder
) {}
