package com.brewmaster.order.dto;

public record BestOfferDto(
        String shopName,
        String shopDomain,
        double price,
        String pricePerUnit,
        String packageSize,
        int packagesNeeded,
        double totalCost
) {}
