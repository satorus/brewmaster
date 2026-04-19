package com.brewmaster.recipe.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record StepRequest(
        @NotNull @Min(1) Integer stepNumber,
        @NotBlank @Size(max = 50) String phase,
        @NotBlank @Size(max = 200) String title,
        @NotBlank String instructions,
        @Min(0) Integer durationMin,
        BigDecimal targetTempC,
        boolean timerRequired,
        String notes
) {}
