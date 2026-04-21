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
                for homebrewing ingredients from German and European online shops using web search.

                Search ONLY these shops — use exactly the domain values listed below:
                - braupartner.de   (shopName: "Braupartner")
                - maischemalzundmehr.de  (shopName: "Maisc he Malz & Mehr")
                - brouwland.com    (shopName: "Brouwland")
                - braumarket.de    (shopName: "Braumarket")

                IMPORTANT RULES:
                1. Use web search to find actual current prices. Do NOT invent prices.
                2. If you cannot find a price for an ingredient via search, set bestOffer to null \
                and explain in searchNote.
                3. Never return a productUrl — only return the shopDomain from the list above.
                4. shopDomain MUST be one of the four values above — no other domains allowed.

                CRITICAL OUTPUT REQUIREMENT:
                Your response must be ONLY a valid JSON object. No markdown. No prose. No code fences. \
                No explanations before or after the JSON. The entire response must be parseable as JSON.

                Required JSON schema (use exactly these field names):
                {
                  "items": [{
                    "ingredientName": "string",
                    "requiredAmount": number,
                    "unit": "string",
                    "searchNote": "string or null",
                    "bestOffer": {
                      "shopName": "string (display name from the list above)",
                      "shopDomain": "string (domain from the list above, e.g. braupartner.de)",
                      "price": number (EUR for required quantity),
                      "pricePerUnit": "string (e.g. '4.90 EUR/kg')",
                      "packageSize": "string (e.g. '1 kg', '500 g')",
                      "packagesNeeded": integer,
                      "totalCost": number
                    },
                    "alternativeOffer": {
                      "shopName": "string",
                      "shopDomain": "string (domain from the list above)",
                      "price": number
                    }
                  }],
                  "estimatedTotalMin": number (sum of all bestOffer totalCost values),
                  "estimatedTotalMax": number (10% above estimatedTotalMin as upper bound),
                  "disclaimer": "Prices are estimates from web search and may vary. Verify on the shop before ordering."
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
