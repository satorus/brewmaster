package com.brewmaster.order.dto;

public record BestOfferDto(
        String shopName,
        double price,
        String pricePerUnit,
        String productUrl,
        String packageSize,
        int packagesNeeded,
        double totalCost
) {}
