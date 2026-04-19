package com.brewmaster.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record AiRecipeDto(
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
        List<AiIngredientDto> ingredients,
        List<AiStepDto> steps
) {}
