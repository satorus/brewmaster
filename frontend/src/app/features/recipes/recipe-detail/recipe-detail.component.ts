import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RecipeService } from '../recipe.service';
import { AuthService } from '../../../core/auth/auth.service';
import { RecipeDetail, IngredientDto, StepDto } from '../../../core/models/recipe.model';

@Component({
  selector: 'app-recipe-detail',
  standalone: true,
  imports: [
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatDividerModule,
    MatChipsModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  template: `
    <mat-toolbar color="primary" class="toolbar">
      <button mat-icon-button (click)="back()"><mat-icon>arrow_back</mat-icon></button>
      <span class="title">{{ recipe()?.name ?? 'Recipe' }}</span>
      <span class="spacer"></span>
      @if (isOwner()) {
        <button mat-icon-button (click)="edit()"><mat-icon>edit</mat-icon></button>
        <button mat-icon-button color="warn" (click)="confirmDelete()"><mat-icon>delete</mat-icon></button>
      }
    </mat-toolbar>

    @if (isLoading()) {
      <div class="center"><mat-spinner></mat-spinner></div>
    } @else if (error()) {
      <div class="center error-state">
        <mat-icon>error_outline</mat-icon>
        <p>{{ error() }}</p>
      </div>
    } @else if (recipe()) {
      <div class="content">

        <!-- Stats bar -->
        <div class="stats-bar">
          @if (recipe()!.style) {
            <div class="stat-item">
              <span class="stat-label">Style</span>
              <span class="stat-value">{{ recipe()!.style }}</span>
            </div>
          }
          @if (recipe()!.abv != null) {
            <div class="stat-item">
              <span class="stat-label">ABV</span>
              <span class="stat-value">{{ recipe()!.abv }}%</span>
            </div>
          }
          @if (recipe()!.ibu != null) {
            <div class="stat-item">
              <span class="stat-label">IBU</span>
              <span class="stat-value">{{ recipe()!.ibu }}</span>
            </div>
          }
          @if (recipe()!.srm != null) {
            <div class="stat-item">
              <span class="stat-label">SRM</span>
              <span class="stat-value">{{ recipe()!.srm }}</span>
            </div>
          }
          <div class="stat-item">
            <span class="stat-label">Volume</span>
            <span class="stat-value">{{ recipe()!.baseVolumeL }}L</span>
          </div>
        </div>

        <!-- Brew parameters -->
        @if (recipe()!.originalGravity || recipe()!.mashTempC || recipe()!.fermentationDays) {
          <mat-card class="section-card">
            <mat-card-content>
              <div class="section-title">Brew Parameters</div>
              <div class="param-grid">
                @if (recipe()!.originalGravity) {
                  <div class="param"><span class="param-label">OG</span><span>{{ recipe()!.originalGravity }}</span></div>
                }
                @if (recipe()!.finalGravity) {
                  <div class="param"><span class="param-label">FG</span><span>{{ recipe()!.finalGravity }}</span></div>
                }
                @if (recipe()!.mashTempC) {
                  <div class="param"><span class="param-label">Mash Temp</span><span>{{ recipe()!.mashTempC }}°C</span></div>
                }
                @if (recipe()!.mashDurationMin) {
                  <div class="param"><span class="param-label">Mash Time</span><span>{{ recipe()!.mashDurationMin }} min</span></div>
                }
                @if (recipe()!.boilDurationMin) {
                  <div class="param"><span class="param-label">Boil Time</span><span>{{ recipe()!.boilDurationMin }} min</span></div>
                }
                @if (recipe()!.fermentationTempC) {
                  <div class="param"><span class="param-label">Ferm Temp</span><span>{{ recipe()!.fermentationTempC }}°C</span></div>
                }
                @if (recipe()!.fermentationDays) {
                  <div class="param"><span class="param-label">Ferm Days</span><span>{{ recipe()!.fermentationDays }}</span></div>
                }
              </div>
            </mat-card-content>
          </mat-card>
        }

        <!-- Description -->
        @if (recipe()!.description) {
          <mat-card class="section-card">
            <mat-card-content>
              <div class="section-title">Description</div>
              <p class="description-text">{{ recipe()!.description }}</p>
            </mat-card-content>
          </mat-card>
        }

        <!-- Ingredients grouped by category -->
        @if (recipe()!.ingredients.length > 0) {
          <mat-card class="section-card">
            <mat-card-content>
              <div class="section-title">Ingredients</div>
              @for (entry of ingredientGroups(); track entry.category) {
                <div class="category-group">
                  <div class="category-label">{{ entry.category }}</div>
                  @for (ing of entry.items; track ing.id) {
                    <div class="ingredient-row">
                      <span class="ing-name">{{ ing.name }}</span>
                      <span class="ing-amount">{{ ing.amount }} {{ ing.unit }}</span>
                    </div>
                    @if (ing.additionTime) {
                      <div class="ing-meta">{{ ing.additionTime }}</div>
                    }
                  }
                </div>
              }
            </mat-card-content>
          </mat-card>
        }

        <!-- Steps grouped by phase -->
        @if (recipe()!.steps.length > 0) {
          <mat-card class="section-card">
            <mat-card-content>
              <div class="section-title">Brew Steps</div>
              @for (entry of stepGroups(); track entry.phase) {
                <div class="phase-group">
                  <div class="phase-label">
                    <mat-chip class="phase-chip">{{ entry.phase }}</mat-chip>
                  </div>
                  @for (step of entry.steps; track step.id) {
                    <div class="step-item">
                      <div class="step-header">
                        <span class="step-num">{{ step.stepNumber }}</span>
                        <span class="step-title">{{ step.title }}</span>
                        @if (step.durationMin) {
                          <span class="step-duration"><mat-icon>timer</mat-icon>{{ step.durationMin }} min</span>
                        }
                      </div>
                      <p class="step-instructions">{{ step.instructions }}</p>
                      @if (step.targetTempC) {
                        <span class="step-temp"><mat-icon>thermostat</mat-icon>{{ step.targetTempC }}°C</span>
                      }
                    </div>
                  }
                </div>
              }
            </mat-card-content>
          </mat-card>
        }

        <!-- Notes -->
        @if (recipe()!.notes) {
          <mat-card class="section-card">
            <mat-card-content>
              <div class="section-title">Notes</div>
              <p class="description-text">{{ recipe()!.notes }}</p>
            </mat-card-content>
          </mat-card>
        }

      </div>
    }
  `,
  styles: [`
    :host { display: block; background: #1A1A1A; min-height: 100%; }
    .toolbar { position: sticky; top: 0; z-index: 10; }
    .title { font-size: 17px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 60vw; }
    .spacer { flex: 1 1 auto; }
    .center { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 60vh; gap: 12px; }
    .error-state { color: #F44336; }
    .content { padding: 12px 16px 80px; display: flex; flex-direction: column; gap: 12px; }

    .stats-bar { display: flex; gap: 0; background: #2C2C2C; border-radius: 8px; overflow: hidden; }
    .stat-item { flex: 1; display: flex; flex-direction: column; align-items: center; padding: 10px 8px; min-width: 0; border-right: 1px solid #333; }
    .stat-item:last-child { border-right: none; }
    .stat-label { font-size: 10px; color: rgba(255,255,255,.4); text-transform: uppercase; letter-spacing: .06em; }
    .stat-value { font-size: 14px; font-weight: 600; color: #C17817; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 100%; text-align: center; }

    .section-card { background: #2C2C2C !important; }
    .section-title { font-size: 13px; font-weight: 600; color: rgba(255,255,255,.5); text-transform: uppercase; letter-spacing: .08em; margin-bottom: 12px; }

    .param-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 8px; }
    .param { display: flex; flex-direction: column; }
    .param-label { font-size: 11px; color: rgba(255,255,255,.4); }

    .description-text { margin: 0; color: rgba(255,255,255,.7); line-height: 1.6; }

    .category-group { margin-bottom: 12px; }
    .category-label { font-size: 11px; font-weight: 700; color: #C17817; text-transform: uppercase; letter-spacing: .08em; margin-bottom: 6px; }
    .ingredient-row { display: flex; justify-content: space-between; padding: 4px 0; border-bottom: 1px solid rgba(255,255,255,.06); }
    .ing-name { font-size: 14px; }
    .ing-amount { font-size: 14px; color: rgba(255,255,255,.6); }
    .ing-meta { font-size: 11px; color: rgba(255,255,255,.35); padding-bottom: 4px; }

    .phase-group { margin-bottom: 16px; }
    .phase-label { margin-bottom: 8px; }
    .phase-chip { background: #3E2723 !important; color: #C17817 !important; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .06em; }
    .step-item { margin-bottom: 14px; padding: 10px; background: rgba(255,255,255,.04); border-radius: 6px; }
    .step-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
    .step-num { font-size: 18px; font-weight: 700; color: #C17817; min-width: 24px; }
    .step-title { font-size: 14px; font-weight: 600; flex: 1; }
    .step-duration { display: flex; align-items: center; gap: 2px; font-size: 12px; color: rgba(255,255,255,.5); }
    .step-duration mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .step-instructions { margin: 0 0 6px; font-size: 13px; color: rgba(255,255,255,.7); line-height: 1.5; }
    .step-temp { display: flex; align-items: center; gap: 2px; font-size: 12px; color: rgba(255,255,255,.5); }
    .step-temp mat-icon { font-size: 14px; width: 14px; height: 14px; }
  `]
})
export class RecipeDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly recipeService = inject(RecipeService);
  private readonly auth = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);

  recipe = signal<RecipeDetail | null>(null);
  isLoading = signal(false);
  error = signal<string | null>(null);

  isOwner = computed(() => this.recipe()?.createdBy === this.auth.currentUser()?.id);

  ingredientGroups = computed(() => {
    const groups = new Map<string, IngredientDto[]>();
    this.recipe()?.ingredients.forEach(i => {
      if (!groups.has(i.category)) groups.set(i.category, []);
      groups.get(i.category)!.push(i);
    });
    return Array.from(groups.entries()).map(([category, items]) => ({ category, items }));
  });

  stepGroups = computed(() => {
    const groups = new Map<string, StepDto[]>();
    this.recipe()?.steps.forEach(s => {
      if (!groups.has(s.phase)) groups.set(s.phase, []);
      groups.get(s.phase)!.push(s);
    });
    return Array.from(groups.entries()).map(([phase, steps]) => ({ phase, steps }));
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.isLoading.set(true);
    this.recipeService.getRecipe(id).subscribe({
      next: r => { this.recipe.set(r); this.isLoading.set(false); },
      error: () => { this.error.set('Failed to load recipe.'); this.isLoading.set(false); },
    });
  }

  back(): void {
    this.router.navigate(['/recipes']);
  }

  edit(): void {
    this.router.navigate(['/recipes', this.recipe()!.id, 'edit']);
  }

  confirmDelete(): void {
    if (!confirm(`Delete "${this.recipe()!.name}"? This cannot be undone.`)) return;
    this.recipeService.deleteRecipe(this.recipe()!.id).subscribe({
      next: () => {
        this.snackBar.open('Recipe deleted', 'Close', { duration: 3000 });
        this.router.navigate(['/recipes']);
      },
      error: () => this.snackBar.open('Failed to delete recipe', 'Close', { duration: 3000 }),
    });
  }
}
