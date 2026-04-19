package com.brewmaster.ai.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record TasteProfileRequest(
        String style,
        @Min(1) @Max(5) Integer bitternessLevel,
        @Min(1) @Max(5) Integer sweetnessLevel,
        String colour,
        @DecimalMin("0.0") @DecimalMax("12.0") BigDecimal targetAbvMin,
        @DecimalMin("0.0") @DecimalMax("12.0") BigDecimal targetAbvMax,
        List<String> aromaNotes,
        @NotNull @DecimalMin("1.0") BigDecimal batchVolumeL,
        String additionalNotes
) {}
