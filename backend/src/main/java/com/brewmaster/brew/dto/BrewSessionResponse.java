package com.brewmaster.brew.dto;

import com.brewmaster.recipe.dto.IngredientDto;
import com.brewmaster.recipe.dto.StepDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BrewSessionResponse(
        UUID id,
        UUID recipeId,
        UUID eventId,
        BigDecimal volumeL,
        int currentStep,
        String status,
        String notes,
        BigDecimal strikeWaterL,
        BigDecimal spargeVolumeL,
        BigDecimal preBoilVolumeL,
        BigDecimal boilOffRatePercent,
        BigDecimal waterToGrainRatio,
        String startedAt,
        String completedAt,
        List<IngredientDto> scaledIngredients,
        List<StepDto> scaledSteps,
        List<StepLogEntry> stepLogs
) {}
