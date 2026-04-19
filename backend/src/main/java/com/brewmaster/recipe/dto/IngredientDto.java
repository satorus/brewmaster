package com.brewmaster.recipe.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record IngredientDto(
        UUID id,
        String name,
        String category,
        BigDecimal amount,
        String unit,
        String additionTime,
        String notes,
        int sortOrder
) {}
