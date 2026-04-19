package com.brewmaster.recipe;

import com.brewmaster.recipe.dto.ScaleRequest;
import com.brewmaster.recipe.dto.ScaledRecipeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test for scaling formulae (spec §8.3).
 *
 * Known inputs:
 *   base recipe = 20L, ingredients: Pale Malt 4.5 kg (MALT), Centennial 30 g (HOP)
 *   targetVolumeL=30, boilOffRatePercent=10, waterToGrainRatio=3.0
 *
 * Expected outputs:
 *   scaleFactor        = 30 / 20 = 1.5
 *   Pale Malt scaled   = 4.5 × 1.5 = 6.750 kg
 *   Centennial scaled  = 30 × 1.5 = 45.000 g
 *   grainWeightKg      = 4.5 × 1.5 = 6.75 kg  (MALT in kg only)
 *   strikeWaterL       = 6.75 × 3.0 = 20.25 L
 *   preBoilVolumeL     = 30 / (1 − 0.10) = 33.33 L
 *   spargeVolumeL      = 33.33 − 20.25 = 13.08 L
 */
@ExtendWith(MockitoExtension.class)
class RecipeServiceScaleTest {

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private RecipeService recipeService;

    private Recipe recipe;

    @BeforeEach
    void setUp() {
        recipe = new Recipe(
                "Test IPA", "American IPA", null, null,
                BigDecimal.valueOf(20),  // base 20L
                null, null, null, null, null, null,
                null, null, null, null, null, false, UUID.randomUUID());

        recipe.getIngredients().add(new RecipeIngredient(
                recipe, "Pale Malt", "MALT", new BigDecimal("4.500"), "kg", null, null, 0));
        recipe.getIngredients().add(new RecipeIngredient(
                recipe, "Centennial", "HOP", new BigDecimal("30.000"), "g", "60 min", null, 1));

        when(recipeRepository.findByIdWithDetails(any())).thenReturn(Optional.of(recipe));
    }

    @Test
    void scale_ingredientsAreLinearlyScaledByVolumeFactor() {
        ScaledRecipeResponse result = recipeService.scaleRecipe(UUID.randomUUID(), scaleRequest());

        assertThat(result.recipe().baseVolumeL())
                .as("scaled volume should be targetVolumeL")
                .isEqualByComparingTo(new BigDecimal("30"));

        assertThat(result.recipe().ingredients().get(0).amount())
                .as("Pale Malt: 4.5 × 1.5 = 6.750")
                .isEqualByComparingTo(new BigDecimal("6.750"));

        assertThat(result.recipe().ingredients().get(1).amount())
                .as("Centennial: 30 × 1.5 = 45.000")
                .isEqualByComparingTo(new BigDecimal("45.000"));
    }

    @Test
    void scale_strikeWaterIsGrainWeightTimesWaterToGrainRatio() {
        ScaledRecipeResponse result = recipeService.scaleRecipe(UUID.randomUUID(), scaleRequest());

        // grainWeightKg = 4.5 × 1.5 = 6.75; strikeWater = 6.75 × 3.0 = 20.25
        assertThat(result.strikeWaterL())
                .as("strikeWaterL = grainWeightKg × waterToGrainRatio = 6.75 × 3.0 = 20.25")
                .isEqualByComparingTo(new BigDecimal("20.25"));
    }

    @Test
    void scale_preBoilVolumeAccountsForBoilOff() {
        ScaledRecipeResponse result = recipeService.scaleRecipe(UUID.randomUUID(), scaleRequest());

        // preBoilVolume = 30 / (1 − 0.10) = 30 / 0.90 = 33.33
        assertThat(result.preBoilVolumeL())
                .as("preBoilVolumeL = targetVolume / (1 − boilOffRate) = 30 / 0.90 = 33.33")
                .isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    void scale_spargeVolumeIsPreBoilMinusStrikeWater() {
        ScaledRecipeResponse result = recipeService.scaleRecipe(UUID.randomUUID(), scaleRequest());

        // spargeVolume = 33.33 − 20.25 = 13.08
        assertThat(result.spargeVolumeL())
                .as("spargeVolumeL = preBoilVolumeL − strikeWaterL = 33.33 − 20.25 = 13.08")
                .isEqualByComparingTo(new BigDecimal("13.08"));
    }

    @Test
    void scale_hopIngredientsDoNotContributeToStrikeWater() {
        // Add a second MALT ingredient to confirm only MALT-in-kg ingredients count
        recipe.getIngredients().add(new RecipeIngredient(
                recipe, "Crystal 60", "MALT", new BigDecimal("0.500"), "kg", null, null, 2));

        ScaledRecipeResponse result = recipeService.scaleRecipe(UUID.randomUUID(), scaleRequest());

        // grainWeightKg now = (4.5 + 0.5) × 1.5 = 7.5; strikeWater = 7.5 × 3.0 = 22.50
        assertThat(result.strikeWaterL())
                .as("strikeWaterL with two MALT ingredients = (4.5 + 0.5) × 1.5 × 3.0 = 22.50")
                .isEqualByComparingTo(new BigDecimal("22.50"));
    }

    @Test
    void scale_spargeVolumeIsNonNegativeWhenStrikeExceedsPreBoil() {
        // Extreme waterToGrainRatio that would produce negative sparge
        ScaleRequest extremeReq = new ScaleRequest(
                BigDecimal.valueOf(10),   // targetVolumeL
                BigDecimal.valueOf(10),   // boilOffRatePercent
                BigDecimal.valueOf(10.0)); // very high ratio → huge strike water

        ScaledRecipeResponse result = recipeService.scaleRecipe(UUID.randomUUID(), extremeReq);

        assertThat(result.spargeVolumeL())
                .as("spargeVolumeL should never be negative")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    // --- helper ---

    private ScaleRequest scaleRequest() {
        return new ScaleRequest(
                BigDecimal.valueOf(30),   // targetVolumeL
                BigDecimal.valueOf(10),   // boilOffRatePercent = 10%
                BigDecimal.valueOf(3.0)); // waterToGrainRatio
    }
}
