package com.brewmaster.recipe.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeSummaryDto(
        UUID id,
        String name,
        String style,
        BigDecimal abv,
        Integer ibu,
        BigDecimal srm,
        BigDecimal baseVolumeL,
        boolean aiGenerated,
        String createdAt
) {}
