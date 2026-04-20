package com.brewmaster.ai;

import com.brewmaster.order.dto.AiOrderResponse;
import com.brewmaster.order.dto.OrderItemDto;
import com.brewmaster.recipe.RecipeService;
import com.brewmaster.recipe.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAiServiceTest {

    @Mock AIClient aiClient;
    @Mock RecipeService recipeService;
    @InjectMocks OrderAiService orderAiService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID recipeId = UUID.randomUUID();

    @Test
    void generateOrderList_returnsAiResponseOnSuccess() throws Exception {
        injectObjectMapper();
        when(recipeService.scaleRecipe(eq(recipeId), any())).thenReturn(buildScaledRecipe());
        when(aiClient.sendWithWebSearch(any())).thenReturn(buildValidAiJson());

        AiOrderResponse result = orderAiService.generateOrderList(recipeId, new BigDecimal("20"));

        assertThat(result.items()).hasSize(1);
        assertThat(result.estimatedTotalMin()).isEqualTo(10.0);
        assertThat(result.estimatedTotalMax()).isEqualTo(12.0);
    }

    @Test
    void generateOrderList_throws503WhenAiClientFails() throws Exception {
        injectObjectMapper();
        when(recipeService.scaleRecipe(eq(recipeId), any())).thenReturn(buildScaledRecipe());
        when(aiClient.sendWithWebSearch(any())).thenThrow(new AIClientException("timeout"));

        assertThatThrownBy(() -> orderAiService.generateOrderList(recipeId, new BigDecimal("20")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Price search temporarily unavailable");
    }

    @Test
    void generateOrderList_throws503WhenResponseIsNotJson() throws Exception {
        injectObjectMapper();
        when(recipeService.scaleRecipe(eq(recipeId), any())).thenReturn(buildScaledRecipe());
        when(aiClient.sendWithWebSearch(any())).thenReturn("Sorry, I cannot help with that.");

        assertThatThrownBy(() -> orderAiService.generateOrderList(recipeId, new BigDecimal("20")))
                .isInstanceOf(ResponseStatusException.class);
    }

    // --- helpers ---

    private void injectObjectMapper() throws Exception {
        var field = OrderAiService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(orderAiService, objectMapper);
    }

    private ScaledRecipeResponse buildScaledRecipe() {
        RecipeResponse recipe = new RecipeResponse(
                recipeId, "Test IPA", "IPA", null, null,
                new BigDecimal("20"), null, null, new BigDecimal("5.5"), 40, null,
                null, null, null, null, null, null,
                false, recipeId, "2025-01-01T00:00:00Z", "2025-01-01T00:00:00Z",
                List.of(new IngredientDto(UUID.randomUUID(), "Pale Malt", "MALT",
                        new BigDecimal("4.5"), "kg", null, null, 1)),
                List.of());
        return new ScaledRecipeResponse(recipe, new BigDecimal("9.0"),
                new BigDecimal("13.0"), new BigDecimal("22.0"));
    }

    private String buildValidAiJson() throws Exception {
        AiOrderResponse response = new AiOrderResponse(
                List.of(new OrderItemDto("Pale Malt", 4.5, "kg", null, null, null)),
                10.0, 12.0, "Prices based on web search.");
        return objectMapper.writeValueAsString(response);
    }
}
