package com.brewmaster.recipe;

import com.brewmaster.auth.dto.LoginRequest;
import com.brewmaster.auth.dto.RegisterRequest;
import com.brewmaster.recipe.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecipeControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private String token;
    private String token2;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("brewer1", "brewer1@test.com", "password123");
        token2 = registerAndLogin("brewer2", "brewer2@test.com", "password123");
    }

    @Test
    void createRecipe_shouldReturn201WithIngredientsAndSteps() throws Exception {
        CreateRecipeRequest req = buildCreateRequest("Test IPA");

        mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test IPA"))
                .andExpect(jsonPath("$.ingredients", hasSize(2)))
                .andExpect(jsonPath("$.steps", hasSize(1)))
                .andExpect(jsonPath("$.ingredients[0].name").value("Pale Malt"))
                .andExpect(jsonPath("$.steps[0].phase").value("MASHING"));
    }

    @Test
    void createRecipe_shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("Unauthorized"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRecipe_shouldReturn400ForMissingName() throws Exception {
        String body = "{\"baseVolumeL\": 20}";
        mockMvc.perform(post("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listRecipes_shouldReturnOnlyOwnRecipes() throws Exception {
        createRecipeAndGetId("My IPA", token);
        createRecipeAndGetId("Other Stout", token2);

        mockMvc.perform(get("/api/v1/recipes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("My IPA"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getRecipeById_shouldReturnFullDetail() throws Exception {
        String id = createRecipeAndGetId("Detail IPA", token);

        mockMvc.perform(get("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.ingredients", hasSize(2)))
                .andExpect(jsonPath("$.steps", hasSize(1)));
    }

    @Test
    void getRecipeById_shouldReturn404ForUnknown() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRecipe_shouldReplaceIngredientsAndSteps() throws Exception {
        String id = createRecipeAndGetId("Original", token);

        UpdateRecipeRequest update = new UpdateRecipeRequest(
                "Updated Stout", "Imperial Stout", null, null,
                BigDecimal.valueOf(25), null, null,
                BigDecimal.valueOf(9.5), 60, null,
                null, null, null, null, null, null,
                List.of(new IngredientRequest("Dark Malt", "MALT", BigDecimal.valueOf(6), "kg", null, null, 0)),
                List.of());

        mockMvc.perform(put("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Stout"))
                .andExpect(jsonPath("$.ingredients", hasSize(1)))
                .andExpect(jsonPath("$.steps", hasSize(0)));
    }

    @Test
    void updateRecipe_shouldReturn403ForNonOwner() throws Exception {
        String id = createRecipeAndGetId("Protected", token);

        UpdateRecipeRequest update = new UpdateRecipeRequest(
                "Hijacked", null, null, null,
                BigDecimal.valueOf(20), null, null,
                null, null, null, null, null, null, null, null, null,
                List.of(), List.of());

        mockMvc.perform(put("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRecipe_shouldReturn204ForOwner() throws Exception {
        String id = createRecipeAndGetId("To Delete", token);

        mockMvc.perform(delete("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRecipe_shouldReturn403ForNonOwner() throws Exception {
        String id = createRecipeAndGetId("Protected Delete", token);

        mockMvc.perform(delete("/api/v1/recipes/" + id)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }

    @Test
    void scaleRecipe_shouldReturnScaledIngredients() throws Exception {
        String id = createRecipeAndGetId("Scale IPA", token);

        ScaleRequest scaleReq = new ScaleRequest(
                BigDecimal.valueOf(40),  // double the 20L base
                BigDecimal.valueOf(10),  // 10% boil-off
                BigDecimal.valueOf(3.0));

        mockMvc.perform(post("/api/v1/recipes/" + id + "/scale")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scaleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipe.baseVolumeL").value(40.0))
                .andExpect(jsonPath("$.recipe.ingredients[0].amount").value(9.0))  // 4.5 * 2
                .andExpect(jsonPath("$.strikeWaterL").isNotEmpty())
                .andExpect(jsonPath("$.preBoilVolumeL").isNotEmpty());
    }

    @Test
    void scaleRecipe_shouldReturn400ForInvalidRequest() throws Exception {
        String id = createRecipeAndGetId("Scale Validation", token);
        String body = "{\"targetVolumeL\": -1, \"boilOffRatePercent\": 10, \"waterToGrainRatio\": 3.0}";

        mockMvc.perform(post("/api/v1/recipes/" + id + "/scale")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- helpers ---

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

    private String createRecipeAndGetId(String name, String authToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/recipes")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildCreateRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private CreateRecipeRequest buildCreateRequest(String name) {
        List<IngredientRequest> ingredients = List.of(
                new IngredientRequest("Pale Malt", "MALT", BigDecimal.valueOf(4.5), "kg", null, "Base malt", 0),
                new IngredientRequest("Centennial", "HOP", BigDecimal.valueOf(30), "g", "60 min", null, 1));

        List<StepRequest> steps = List.of(
                new StepRequest(1, "MASHING", "Mash in", "Add grain to 15L water at 72°C.",
                        60, BigDecimal.valueOf(66.5), false, null));

        return new CreateRecipeRequest(
                name, "American IPA", "A hoppy IPA", null,
                BigDecimal.valueOf(20), BigDecimal.valueOf(1.062), BigDecimal.valueOf(1.012),
                BigDecimal.valueOf(6.5), 65, BigDecimal.valueOf(6.0),
                BigDecimal.valueOf(66.5), 60, 60,
                BigDecimal.valueOf(19.0), 14, null,
                ingredients, steps);
    }
}
