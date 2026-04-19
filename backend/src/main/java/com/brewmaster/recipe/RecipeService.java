package com.brewmaster.recipe;

import com.brewmaster.recipe.dto.*;
import com.brewmaster.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    @Transactional(readOnly = true)
    public Page<RecipeSummaryDto> listRecipes(UUID userId, Pageable pageable) {
        return recipeRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable)
                .map(this::toSummary);
    }

    @Transactional
    public RecipeResponse createRecipe(CreateRecipeRequest req, User user) {
        Recipe recipe = new Recipe(
                req.name(), req.style(), req.description(), req.sourceUrl(),
                req.baseVolumeL(), req.originalGravity(), req.finalGravity(),
                req.abv(), req.ibu(), req.srm(), req.mashTempC(),
                req.mashDurationMin(), req.boilDurationMin(),
                req.fermentationTempC(), req.fermentationDays(),
                req.notes(), false, user.getId());

        addChildren(recipe, req.ingredients(), req.steps());
        return toResponse(recipeRepository.save(recipe));
    }

    @Transactional(readOnly = true)
    public RecipeResponse getRecipe(UUID id) {
        Recipe recipe = findOrThrow(id);
        return toResponse(recipe);
    }

    @Transactional
    public RecipeResponse updateRecipe(UUID id, UpdateRecipeRequest req, User user) {
        Recipe recipe = findOrThrow(id);
        requireOwner(recipe, user);

        recipe.update(req.name(), req.style(), req.description(), req.sourceUrl(),
                req.baseVolumeL(), req.originalGravity(), req.finalGravity(),
                req.abv(), req.ibu(), req.srm(), req.mashTempC(),
                req.mashDurationMin(), req.boilDurationMin(),
                req.fermentationTempC(), req.fermentationDays(), req.notes());

        recipe.getIngredients().clear();
        recipe.getSteps().clear();
        addChildren(recipe, req.ingredients(), req.steps());

        return toResponse(recipeRepository.save(recipe));
    }

    @Transactional
    public void deleteRecipe(UUID id, User user) {
        Recipe recipe = findOrThrow(id);
        requireOwner(recipe, user);
        recipeRepository.delete(recipe);
    }

    @Transactional(readOnly = true)
    public ScaledRecipeResponse scaleRecipe(UUID id, ScaleRequest req) {
        Recipe recipe = findOrThrow(id);

        BigDecimal targetVolume = req.targetVolumeL();
        BigDecimal scaleFactor = targetVolume.divide(recipe.getBaseVolumeL(), 10, RoundingMode.HALF_UP);

        List<IngredientDto> scaledIngredients = recipe.getIngredients().stream()
                .map(i -> new IngredientDto(
                        i.getId(), i.getName(), i.getCategory(),
                        i.getAmount().multiply(scaleFactor).setScale(3, RoundingMode.HALF_UP),
                        i.getUnit(), i.getAdditionTime(), i.getNotes(), i.getSortOrder()))
                .toList();

        // Strike water from MALT ingredients (kg only)
        BigDecimal grainWeightKg = recipe.getIngredients().stream()
                .filter(i -> "MALT".equalsIgnoreCase(i.getCategory()) && "kg".equalsIgnoreCase(i.getUnit()))
                .map(i -> i.getAmount().multiply(scaleFactor))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal strikeWaterL = grainWeightKg.multiply(req.waterToGrainRatio())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal boilOffRate = req.boilOffRatePercent()
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal preBoilVolumeL = targetVolume
                .divide(BigDecimal.ONE.subtract(boilOffRate), 2, RoundingMode.HALF_UP);

        BigDecimal spargeVolumeL = preBoilVolumeL.subtract(strikeWaterL)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        List<StepDto> steps = recipe.getSteps().stream().map(this::toStepDto).toList();

        RecipeResponse scaledRecipe = new RecipeResponse(
                recipe.getId(), recipe.getName(), recipe.getStyle(),
                recipe.getDescription(), recipe.getSourceUrl(), targetVolume,
                recipe.getOriginalGravity(), recipe.getFinalGravity(),
                recipe.getAbv(), recipe.getIbu(), recipe.getSrm(),
                recipe.getMashTempC(), recipe.getMashDurationMin(),
                recipe.getBoilDurationMin(), recipe.getFermentationTempC(),
                recipe.getFermentationDays(), recipe.getNotes(),
                recipe.isAiGenerated(), recipe.getCreatedBy(),
                recipe.getCreatedAt().toString(), recipe.getUpdatedAt().toString(),
                scaledIngredients, steps);

        return new ScaledRecipeResponse(scaledRecipe, strikeWaterL, spargeVolumeL, preBoilVolumeL);
    }

    // --- helpers ---

    private Recipe findOrThrow(UUID id) {
        return recipeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipe not found"));
    }

    private void requireOwner(Recipe recipe, User user) {
        if (!recipe.getCreatedBy().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipe creator can modify it");
        }
    }

    private void addChildren(Recipe recipe, List<IngredientRequest> ingredients, List<StepRequest> steps) {
        if (ingredients != null) {
            ingredients.forEach(r -> recipe.getIngredients().add(
                    new RecipeIngredient(recipe, r.name(), r.category(), r.amount(),
                            r.unit(), r.additionTime(), r.notes(), r.sortOrder())));
        }
        if (steps != null) {
            steps.forEach(s -> recipe.getSteps().add(
                    new RecipeStep(recipe, s.stepNumber(), s.phase(), s.title(),
                            s.instructions(), s.durationMin(), s.targetTempC(),
                            s.timerRequired(), s.notes())));
        }
    }

    private RecipeSummaryDto toSummary(Recipe r) {
        return new RecipeSummaryDto(r.getId(), r.getName(), r.getStyle(),
                r.getAbv(), r.getIbu(), r.getSrm(), r.getBaseVolumeL(),
                r.isAiGenerated(), r.getCreatedAt().toString());
    }

    private RecipeResponse toResponse(Recipe r) {
        return new RecipeResponse(
                r.getId(), r.getName(), r.getStyle(), r.getDescription(), r.getSourceUrl(),
                r.getBaseVolumeL(), r.getOriginalGravity(), r.getFinalGravity(),
                r.getAbv(), r.getIbu(), r.getSrm(), r.getMashTempC(),
                r.getMashDurationMin(), r.getBoilDurationMin(),
                r.getFermentationTempC(), r.getFermentationDays(), r.getNotes(),
                r.isAiGenerated(), r.getCreatedBy(),
                r.getCreatedAt().toString(), r.getUpdatedAt().toString(),
                r.getIngredients().stream().map(this::toIngredientDto).toList(),
                r.getSteps().stream().map(this::toStepDto).toList());
    }

    private IngredientDto toIngredientDto(RecipeIngredient i) {
        return new IngredientDto(i.getId(), i.getName(), i.getCategory(),
                i.getAmount(), i.getUnit(), i.getAdditionTime(), i.getNotes(), i.getSortOrder());
    }

    private StepDto toStepDto(RecipeStep s) {
        return new StepDto(s.getId(), s.getStepNumber(), s.getPhase(), s.getTitle(),
                s.getInstructions(), s.getDurationMin(), s.getTargetTempC(),
                s.isTimerRequired(), s.getNotes());
    }
}
