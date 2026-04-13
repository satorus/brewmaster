package com.brewmaster.recipe;

import org.springframework.stereotype.Service;

// TODO: implement in Recipe feature milestone
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;

    public RecipeService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }
}
