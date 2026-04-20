package com.brewmaster.order.dto;

public record AlternativeOfferDto(
        String shopName,
        double price,
        String productUrl
) {}
