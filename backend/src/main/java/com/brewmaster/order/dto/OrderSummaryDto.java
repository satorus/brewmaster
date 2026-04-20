package com.brewmaster.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderSummaryDto(
        UUID id,
        String recipeName,
        BigDecimal volumeL,
        double estimatedTotalMin,
        double estimatedTotalMax,
        String generatedAt
) {}
