package com.brewmaster.recipe.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ScaleRequest(
        @NotNull @DecimalMin("0.1") BigDecimal targetVolumeL,
        @NotNull @DecimalMin("0") @DecimalMax("50") BigDecimal boilOffRatePercent,
        @NotNull @DecimalMin("1.0") @DecimalMax("10.0") BigDecimal waterToGrainRatio
) {}
