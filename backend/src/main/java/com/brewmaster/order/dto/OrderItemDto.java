package com.brewmaster.order.dto;

public record OrderItemDto(
        String ingredientName,
        double requiredAmount,
        String unit,
        String searchNote,
        BestOfferDto bestOffer,
        AlternativeOfferDto alternativeOffer
) {}
