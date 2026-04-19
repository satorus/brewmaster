import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RecipeCardComponent } from '../../shared/components/recipe-card/recipe-card.component';
import { RecipeService } from './recipe.service';
import { RecipeSummary } from '../../core/models/recipe.model';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-recipes',
  standalone: true,
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    RecipeCardComponent,
  ],
  template: `
    <mat-toolbar color="primary" class="toolbar">
      <span class="title">🍺 My Recipes</span>
      <span class="spacer"></span>
      <button mat-icon-button (click)="auth.logout()" matTooltip="Sign out">
        <mat-icon>logout</mat-icon>
      </button>
    </mat-toolbar>

    @if (isLoading()) {
      <div class="center">
        <mat-spinner></mat-spinner>
      </div>
    } @else if (error()) {
      <div class="center error-state">
        <mat-icon>error_outline</mat-icon>
        <p>{{ error() }}</p>
        <button mat-flat-button color="primary" (click)="load()">Retry</button>
      </div>
    } @else {
      <div class="list-container">
        @if (recipes().length === 0) {
          <div class="empty-state">
            <mat-icon>menu_book</mat-icon>
            <p>No recipes yet</p>
            <button mat-flat-button color="primary" (click)="goToNew()">Add First Recipe</button>
          </div>
        }
        @for (recipe of recipes(); track recipe.id) {
          <app-recipe-card [recipe]="recipe" (clicked)="goToDetail($event)" />
        }
      </div>
    }

    <button mat-fab class="fab" color="primary" (click)="goToNew()" aria-label="Add recipe">
      <mat-icon>add</mat-icon>
    </button>
  `,
  styles: [`
    :host { display: block; background: #1A1A1A; min-height: 100%; }
    .toolbar { position: sticky; top: 0; z-index: 10; }
    .title { font-size: 18px; font-weight: 600; }
    .spacer { flex: 1 1 auto; }
    .list-container { padding: 12px 16px 80px; }
    .center { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 60vh; gap: 12px; }
    .error-state { color: #F44336; }
    .error-state mat-icon { font-size: 48px; width: 48px; height: 48px; }
    .empty-state { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; height: 50vh; color: rgba(255,255,255,.4); }
    .empty-state mat-icon { font-size: 64px; width: 64px; height: 64px; }
    .fab { position: fixed; bottom: 72px; right: 24px; z-index: 20; }
  `]
})
export class RecipesComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly recipeService = inject(RecipeService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  recipes = signal<RecipeSummary[]>([]);
  isLoading = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.recipeService.getRecipes().subscribe({
      next: page => {
        this.recipes.set(page.content);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load recipes. Please try again.');
        this.isLoading.set(false);
      },
    });
  }

  goToDetail(recipe: RecipeSummary): void {
    this.router.navigate(['/recipes', recipe.id]);
  }

  goToNew(): void {
    this.router.navigate(['/recipes/new']);
  }
}
