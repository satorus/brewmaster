package com.brewmaster.recipe.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StepDto(
        UUID id,
        int stepNumber,
        String phase,
        String title,
        String instructions,
        Integer durationMin,
        BigDecimal targetTempC,
        boolean timerRequired,
        String notes
) {}
