package com.brewmaster.brew.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdvanceStepRequest(
        @NotNull Integer stepNumber,
        BigDecimal actualTempC,
        String notes
) {}
