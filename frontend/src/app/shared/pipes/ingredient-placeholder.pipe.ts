import { Pipe, PipeTransform } from '@angular/core';
import { IngredientDto } from '../../core/models/recipe.model';

@Pipe({ name: 'ingredientPlaceholder', standalone: true, pure: true })
export class IngredientPlaceholderPipe implements PipeTransform {
  transform(instructions: string, ingredients: IngredientDto[]): string {
    if (!instructions || !ingredients?.length) return instructions;
    return instructions.replace(/\{ingredient_(\d+)\}/g, (match, idx) => {
      const ing = ingredients[parseInt(idx, 10)];
      return ing ? `${ing.amount} ${ing.unit}` : match;
    });
  }
}
