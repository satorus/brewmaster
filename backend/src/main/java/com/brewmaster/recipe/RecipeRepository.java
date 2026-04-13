package com.brewmaster.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// TODO: add custom queries for recipe feature
public interface RecipeRepository extends JpaRepository<Recipe, UUID> {}
