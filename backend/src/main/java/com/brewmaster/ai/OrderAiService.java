package com.brewmaster.ai;

import com.brewmaster.order.dto.AiOrderResponse;
import com.brewmaster.recipe.RecipeService;
import com.brewmaster.recipe.dto.IngredientDto;
import com.brewmaster.recipe.dto.ScaleRequest;
import com.brewmaster.recipe.dto.ScaledRecipeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderAiService {

    private static final Logger log = LoggerFactory.getLogger(OrderAiService.class);

    private final AIClient aiClient;
    private final RecipeService recipeService;
    private final ObjectMapper objectMapper;

    public OrderAiService(AIClient aiClient, RecipeService recipeService, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.recipeService = recipeService;
        this.objectMapper = objectMapper;
    }

    public AiOrderResponse generateOrderList(UUID recipeId, BigDecimal volumeL) {
        log.debug("Generating order list for recipe={}, volume={}L", recipeId, volumeL);

        ScaledRecipeResponse scaled = recipeService.scaleRecipe(recipeId,
                new ScaleRequest(volumeL, BigDecimal.TEN, new BigDecimal("3.0")));

        AIRequest request = new AIRequest(buildSystemPrompt(),
                buildUserMessage(scaled.recipe().name(), scaled.recipe().ingredients(), volumeL), 8000);

        String rawResponse;
        try {
            rawResponse = aiClient.sendWithWebSearch(request);
        } catch (AIClientException e) {
            log.error("AI client error during order list generation: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Price search temporarily unavailable. Please try again.");
        }

        String json;
        try {
            json = AIJsonUtil.extractJson(rawResponse, objectMapper);
        } catch (AIClientException e) {
            log.error("Failed to extract JSON from AI order response: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI returned an unexpected response format. Please try again.");
        }

        try {
            AiOrderResponse result = objectMapper.readValue(json, AiOrderResponse.class);
            log.debug("AI order list generated {} items, total {}-{} EUR",
                    result.items().size(), result.estimatedTotalMin(), result.estimatedTotalMax());
            return result;
        } catch (Exception e) {
            log.error("Failed to deserialize AI order response: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI returned an unexpected response format. Please try again.");
        }
    }

    private String buildSystemPrompt() {
        return """
                You are a homebrewing supply assistant. Your task is to find the best current prices \
                for homebrewing ingredients from German and European online shops.

                Search these shops for each ingredient:
                - braupartner.de
                - hobbybrauer.de
                - maischemalzundmehr.de
                - brouwland.com
                - brewup.eu

                For each ingredient:
                1. Search the shop websites for the exact product or a close equivalent
                2. Find the best price per unit (cheapest shop)
                3. Find one alternative offer from a different shop
                4. Calculate how many packages are needed and the total cost

                CRITICAL OUTPUT REQUIREMENT:
                Your response must be ONLY a valid JSON object. No markdown. No prose. No code fences. \
                No explanations before or after the JSON. The entire response must be parseable as JSON.

                Required JSON schema (use exactly these field names):
                {
                  "items": [{
                    "ingredientName": "string",
                    "requiredAmount": number,
                    "unit": "string",
                    "searchNote": "string (any notes about substitutions or availability)",
                    "bestOffer": {
                      "shopName": "string",
                      "price": number (price in EUR for required quantity),
                      "pricePerUnit": "string (e.g. '4.90 EUR/kg')",
                      "productUrl": "string (direct URL to product)",
                      "packageSize": "string (e.g. '1 kg', '500 g')",
                      "packagesNeeded": integer,
                      "totalCost": number
                    },
                    "alternativeOffer": {
                      "shopName": "string",
                      "price": number,
                      "productUrl": "string"
                    }
                  }],
                  "estimatedTotalMin": number (sum of all bestOffer totalCost values),
                  "estimatedTotalMax": number (sum of all alternativeOffer prices),
                  "disclaimer": "string (note about price accuracy and date)"
                }
                """;
    }

    private String buildUserMessage(String recipeName, List<IngredientDto> ingredients, BigDecimal volumeL) {
        var sb = new StringBuilder();
        sb.append("Find prices for these homebrewing ingredients for brewing ")
          .append(recipeName).append(" (").append(volumeL).append("L batch):\n\n");

        for (IngredientDto ing : ingredients) {
            sb.append("- ").append(ing.name())
              .append(": ").append(ing.amount())
              .append(" ").append(ing.unit())
              .append(" [").append(ing.category()).append("]");
            if (ing.notes() != null && !ing.notes().isBlank()) {
                sb.append(" (").append(ing.notes()).append(")");
            }
            sb.append("\n");
        }

        sb.append("\nSearch each shop for the best available price. Respond with ONLY the JSON object.");
        return sb.toString();
    }
}
