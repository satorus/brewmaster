package com.brewmaster.brew.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record StartSessionRequest(
        @NotNull UUID recipeId,
        UUID eventId,
        @NotNull @DecimalMin("1") BigDecimal targetVolumeL,
        @DecimalMin("0") @DecimalMax("30") BigDecimal boilOffRatePercent,
        @DecimalMin("1") @DecimalMax("10") BigDecimal waterToGrainRatio,
        String notes
) {}
