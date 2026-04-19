package com.brewmaster.recipe.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CreateRecipeRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 100) String style,
        String description,
        String sourceUrl,
        @NotNull @DecimalMin("0.1") BigDecimal baseVolumeL,
        BigDecimal originalGravity,
        BigDecimal finalGravity,
        @DecimalMin("0") @DecimalMax("100") BigDecimal abv,
        @Min(0) Integer ibu,
        BigDecimal srm,
        BigDecimal mashTempC,
        @Min(0) Integer mashDurationMin,
        @Min(0) Integer boilDurationMin,
        BigDecimal fermentationTempC,
        @Min(0) Integer fermentationDays,
        String notes,
        @Valid List<IngredientRequest> ingredients,
        @Valid List<StepRequest> steps,
        Boolean aiGenerated
) {}
