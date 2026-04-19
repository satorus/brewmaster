package com.brewmaster.recipe.dto;

import java.math.BigDecimal;

public record ScaledRecipeResponse(
        RecipeResponse recipe,
        BigDecimal strikeWaterL,
        BigDecimal spargeVolumeL,
        BigDecimal preBoilVolumeL
) {}
