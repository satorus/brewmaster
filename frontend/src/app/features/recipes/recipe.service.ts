import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/api/api.service';
import {
  PageResponse,
  RecipeDetail,
  RecipeSummary,
  SaveRecipeRequest,
  ScaleRequest,
  ScaledRecipeResponse,
} from '../../core/models/recipe.model';

@Injectable({ providedIn: 'root' })
export class RecipeService {
  private readonly api = inject(ApiService);

  getRecipes(page = 0, size = 50): Observable<PageResponse<RecipeSummary>> {
    return this.api.get<PageResponse<RecipeSummary>>('/recipes', {
      page: String(page),
      size: String(size),
    });
  }

  getRecipe(id: string): Observable<RecipeDetail> {
    return this.api.get<RecipeDetail>(`/recipes/${id}`);
  }

  createRecipe(req: SaveRecipeRequest): Observable<RecipeDetail> {
    return this.api.post<RecipeDetail>('/recipes', req);
  }

  updateRecipe(id: string, req: SaveRecipeRequest): Observable<RecipeDetail> {
    return this.api.put<RecipeDetail>(`/recipes/${id}`, req);
  }

  deleteRecipe(id: string): Observable<void> {
    return this.api.delete<void>(`/recipes/${id}`);
  }

  scaleRecipe(id: string, req: ScaleRequest): Observable<ScaledRecipeResponse> {
    return this.api.post<ScaledRecipeResponse>(`/recipes/${id}/scale`, req);
  }
}
