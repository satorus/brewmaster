-- V3: Performance indexes for recipe module
-- Tables (recipes, recipe_ingredients, recipe_steps) were created in V1
CREATE INDEX idx_recipes_created_by ON recipes(created_by);
CREATE INDEX idx_recipe_ingredients_recipe_id ON recipe_ingredients(recipe_id, sort_order);
CREATE INDEX idx_recipe_steps_recipe_id ON recipe_steps(recipe_id, step_number);
