package com.brewmaster.order.dto;

import java.util.List;

public record AiOrderResponse(
        List<OrderItemDto> items,
        double estimatedTotalMin,
        double estimatedTotalMax,
        String disclaimer
) {}
