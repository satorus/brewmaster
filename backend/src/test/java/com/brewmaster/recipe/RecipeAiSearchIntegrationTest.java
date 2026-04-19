package com.brewmaster.recipe;

import com.brewmaster.ai.RecipeAiService;
import com.brewmaster.ai.dto.AiIngredientDto;
import com.brewmaster.ai.dto.AiRecipeDto;
import com.brewmaster.ai.dto.AiRecipeSearchResponse;
import com.brewmaster.ai.dto.AiStepDto;
import com.brewmaster.auth.dto.LoginRequest;
import com.brewmaster.auth.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecipeAiSearchIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean
    RecipeAiService recipeAiService;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("aisearcher", "ai@test.com", "password123");
    }

    @Test
    void aiSearch_returns200WithResults() throws Exception {
        AiRecipeSearchResponse mockResponse = new AiRecipeSearchResponse(List.of(buildMockRecipe()));
        when(recipeAiService.findRecipes(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/recipes/ai-search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidProfile())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipes").isArray())
                .andExpect(jsonPath("$.recipes[0].name").value("Test IPA"));
    }

    @Test
    void aiSearch_returns400ForMissingBatchVolume() throws Exception {
        String body = "{\"style\":\"IPA\",\"bitternessLevel\":3,\"sweetnessLevel\":3,\"colour\":\"Pale\"}";

        mockMvc.perform(post("/api/v1/recipes/ai-search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aiSearch_returns503WhenServiceUnavailable() throws Exception {
        when(recipeAiService.findRecipes(any()))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "AI service temporarily unavailable."));

        mockMvc.perform(post("/api/v1/recipes/ai-search")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidProfile())))
                .andExpect(status().isServiceUnavailable());
    }

    private String registerAndLogin(String username, String email, String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest(username, email, password, null))))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(username, password))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private Object buildValidProfile() {
        return new java.util.LinkedHashMap<>() {{
            put("style", "IPA");
            put("bitternessLevel", 3);
            put("sweetnessLevel", 3);
            put("colour", "Pale");
            put("targetAbvMin", 4.5);
            put("targetAbvMax", 6.5);
            put("aromaNotes", List.of("Citrus"));
            put("batchVolumeL", 20);
        }};
    }

    private AiRecipeDto buildMockRecipe() {
        return new AiRecipeDto(
                "Test IPA", "American IPA", "A test IPA.", "https://example.com",
                new BigDecimal("20"), new BigDecimal("1.062"), new BigDecimal("1.012"),
                new BigDecimal("6.5"), 65, new BigDecimal("6"),
                new BigDecimal("66.5"), 60, 60,
                new BigDecimal("19"), 14,
                List.of(new AiIngredientDto("Pale Malt", "MALT", new BigDecimal("4.5"), "kg", null, null, 0)),
                List.of(new AiStepDto(1, "MASHING", "Mash In", "Add grain.", 60, new BigDecimal("66.5"), false, null))
        );
    }
}
