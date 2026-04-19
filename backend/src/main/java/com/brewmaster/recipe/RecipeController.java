package com.brewmaster.recipe;

import com.brewmaster.ai.RecipeAiService;
import com.brewmaster.ai.dto.AiRecipeSearchResponse;
import com.brewmaster.ai.dto.TasteProfileRequest;
import com.brewmaster.recipe.dto.*;
import com.brewmaster.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipes")
@Tag(name = "Recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final RecipeAiService recipeAiService;

    public RecipeController(RecipeService recipeService, RecipeAiService recipeAiService) {
        this.recipeService = recipeService;
        this.recipeAiService = recipeAiService;
    }

    @GetMapping
    @Operation(summary = "List current user's recipes (paginated)")
    public ResponseEntity<Page<RecipeSummaryDto>> list(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(recipeService.listRecipes(user.getId(), pageable));
    }

    @PostMapping
    @Operation(summary = "Create a new recipe")
    public ResponseEntity<RecipeResponse> create(
            @Valid @RequestBody CreateRecipeRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeService.createRecipe(req, user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full recipe detail")
    public ResponseEntity<RecipeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(recipeService.getRecipe(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update recipe (owner only) — replaces ingredients and steps")
    public ResponseEntity<RecipeResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecipeRequest req,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, req, user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete recipe (owner only)")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        recipeService.deleteRecipe(id, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ai-search")
    @Operation(summary = "AI recipe finder — searches the web for recipes matching the taste profile")
    public ResponseEntity<AiRecipeSearchResponse> aiSearch(
            @Valid @RequestBody TasteProfileRequest req) {
        return ResponseEntity.ok(recipeAiService.findRecipes(req));
    }

    @PostMapping("/{id}/scale")
    @Operation(summary = "Scale recipe to a new volume — returns a scaled copy, not persisted")
    public ResponseEntity<ScaledRecipeResponse> scale(
            @PathVariable UUID id,
            @Valid @RequestBody ScaleRequest req) {
        return ResponseEntity.ok(recipeService.scaleRecipe(id, req));
    }
}
