package com.brewmaster.recipe.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecipeResponse(
        UUID id,
        String name,
        String style,
        String description,
        String sourceUrl,
        BigDecimal baseVolumeL,
        BigDecimal originalGravity,
        BigDecimal finalGravity,
        BigDecimal abv,
        Integer ibu,
        BigDecimal srm,
        BigDecimal mashTempC,
        Integer mashDurationMin,
        Integer boilDurationMin,
        BigDecimal fermentationTempC,
        Integer fermentationDays,
        String notes,
        boolean aiGenerated,
        UUID createdBy,
        String createdAt,
        String updatedAt,
        List<IngredientDto> ingredients,
        List<StepDto> steps
) {}
