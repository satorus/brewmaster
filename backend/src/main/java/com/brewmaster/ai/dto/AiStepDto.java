package com.brewmaster.ai.dto;

import java.math.BigDecimal;

public record AiStepDto(
        int stepNumber,
        String phase,
        String title,
        String instructions,
        Integer durationMin,
        BigDecimal targetTempC,
        boolean timerRequired,
        String notes
) {}
