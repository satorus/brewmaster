package com.brewmaster.recipe.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record IngredientRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String category,
        @NotNull @DecimalMin("0.001") BigDecimal amount,
        @NotBlank @Size(max = 20) String unit,
        @Size(max = 100) String additionTime,
        String notes,
        int sortOrder
) {}
