package com.brewmaster.brew.dto;

import java.math.BigDecimal;

public record StepLogEntry(
        int stepNumber,
        String completedAt,
        BigDecimal actualTempC,
        String notes
) {}
