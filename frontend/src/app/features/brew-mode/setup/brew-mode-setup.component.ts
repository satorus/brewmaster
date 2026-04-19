import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AsyncPipe, DecimalPipe } from '@angular/common';
import { map, startWith } from 'rxjs/operators';
import { RecipeService } from '../../recipes/recipe.service';
import { BrewSessionService } from '../brew-session.service';
import { RecipeSummary } from '../../../core/models/recipe.model';

@Component({
  selector: 'app-brew-mode-setup',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatToolbarModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatAutocompleteModule,
    MatProgressSpinnerModule, MatCardModule, MatSnackBarModule,
    AsyncPipe, DecimalPipe,
  ],
  template: `
    <mat-toolbar color="primary">
      <button mat-icon-button (click)="back()"><mat-icon>arrow_back</mat-icon></button>
      <span>Start Brew Session</span>
    </mat-toolbar>

    @if (loadingRecipes()) {
      <div class="center"><mat-spinner diameter="40"></mat-spinner></div>
    } @else {
      <div class="setup-container">
        <form [formGroup]="form" (ngSubmit)="start()">
          <mat-card class="setup-card">
            <mat-card-header><mat-card-title>Session Setup</mat-card-title></mat-card-header>
            <mat-card-content>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Recipe</mat-label>
                <input matInput formControlName="recipeName" [matAutocomplete]="auto"
                       placeholder="Search your recipes...">
                <mat-autocomplete #auto="matAutocomplete" [displayWith]="displayRecipe"
                                  (optionSelected)="onRecipeSelected($event.option.value)">
                  @for (r of filteredRecipes(); track r.id) {
                    <mat-option [value]="r">{{ r.name }} ({{ r.style ?? 'No style' }})</mat-option>
                  }
                </mat-autocomplete>
                @if (form.get('recipeName')?.invalid && form.get('recipeName')?.touched) {
                  <mat-error>Please select a recipe</mat-error>
                }
              </mat-form-field>

              <div class="row">
                <mat-form-field appearance="outline">
                  <mat-label>Target Volume (L)</mat-label>
                  <input matInput type="number" formControlName="targetVolumeL" min="1">
                  @if (form.get('targetVolumeL')?.invalid && form.get('targetVolumeL')?.touched) {
                    <mat-error>Minimum 1L</mat-error>
                  }
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Boil-off Rate (%)</mat-label>
                  <input matInput type="number" formControlName="boilOffRatePercent" min="0" max="30">
                </mat-form-field>

                <mat-form-field appearance="outline">
                  <mat-label>Water:Grain Ratio</mat-label>
                  <input matInput type="number" formControlName="waterToGrainRatio" min="1" max="10" step="0.1">
                </mat-form-field>
              </div>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Notes (optional)</mat-label>
                <textarea matInput formControlName="notes" rows="2"></textarea>
              </mat-form-field>

            </mat-card-content>
          </mat-card>

          @if (preview()) {
            <mat-card class="preview-card">
              <mat-card-header><mat-card-title>Water Volumes Preview</mat-card-title></mat-card-header>
              <mat-card-content>
                <div class="preview-grid">
                  <span>Strike Water</span><strong>{{ preview()!.strikeWater | number:'1.1-2' }} L</strong>
                  <span>Pre-boil Volume</span><strong>{{ preview()!.preBoil | number:'1.1-2' }} L</strong>
                  <span>Sparge Volume</span><strong>{{ preview()!.sparge | number:'1.1-2' }} L</strong>
                </div>
              </mat-card-content>
            </mat-card>
          }

          <button mat-raised-button color="primary" type="submit"
                  [disabled]="form.invalid || starting() || !selectedRecipeId()"
                  class="start-btn">
            @if (starting()) {
              <mat-spinner diameter="20"></mat-spinner>
            } @else {
              <mat-icon>science</mat-icon> Start Brewing
            }
          </button>
        </form>
      </div>
    }
  `,
  styles: [`
    .setup-container { padding: 16px; max-width: 600px; margin: 0 auto; }
    .setup-card, .preview-card { margin-bottom: 16px; }
    .full-width { width: 100%; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; }
    .row mat-form-field { flex: 1; min-width: 120px; }
    .preview-grid { display: grid; grid-template-columns: 1fr auto; gap: 8px 16px; }
    .start-btn { width: 100%; height: 48px; font-size: 16px; }
    .center { display: flex; justify-content: center; padding: 40px; }
    mat-spinner { display: inline-block; }
  `]
})
export class BrewModeSetupComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private recipeService = inject(RecipeService);
  private sessionService = inject(BrewSessionService);
  private snackBar = inject(MatSnackBar);

  readonly loadingRecipes = signal(true);
  readonly starting = signal(false);
  readonly recipes = signal<RecipeSummary[]>([]);
  readonly selectedRecipeId = signal<string | null>(null);

  readonly form = this.fb.group({
    recipeName: ['', Validators.required],
    targetVolumeL: [20, [Validators.required, Validators.min(1)]],
    boilOffRatePercent: [10, [Validators.min(0), Validators.max(30)]],
    waterToGrainRatio: [3.0, [Validators.min(1), Validators.max(10)]],
    notes: [''],
  });

  readonly filteredRecipes = computed(() => {
    const term = (this.form.get('recipeName')?.value ?? '').toLowerCase();
    return this.recipes().filter(r => r.name.toLowerCase().includes(term));
  });

  readonly preview = computed(() => {
    const v = this.form.get('targetVolumeL')?.value ?? 20;
    const b = (this.form.get('boilOffRatePercent')?.value ?? 10) / 100;
    const wg = this.form.get('waterToGrainRatio')?.value ?? 3.0;
    if (!this.selectedRecipeId()) return null;
    const recipe = this.recipes().find(r => r.id === this.selectedRecipeId());
    if (!recipe) return null;
    const preBoil = v / (1 - b);
    const strikeWater = 3 * wg; // simplified estimate without grain weight
    const sparge = Math.max(0, preBoil - strikeWater);
    return { strikeWater, preBoil, sparge };
  });

  ngOnInit(): void {
    this.recipeService.getRecipes(0, 200).subscribe({
      next: page => {
        this.recipes.set(page.content);
        this.loadingRecipes.set(false);
        const recipeId = this.route.snapshot.queryParamMap.get('recipeId');
        if (recipeId) {
          const recipe = page.content.find(r => r.id === recipeId);
          if (recipe) {
            this.form.patchValue({ recipeName: recipe.name });
            this.selectedRecipeId.set(recipeId);
          }
        }
      },
      error: () => this.loadingRecipes.set(false),
    });
  }

  onRecipeSelected(recipe: RecipeSummary): void {
    this.selectedRecipeId.set(recipe.id);
    this.form.patchValue({ targetVolumeL: recipe.baseVolumeL });
  }

  displayRecipe(recipe: RecipeSummary | string | null): string {
    if (!recipe) return '';
    if (typeof recipe === 'string') return recipe;
    return recipe.name;
  }

  start(): void {
    if (this.form.invalid || !this.selectedRecipeId()) return;
    this.starting.set(true);
    const v = this.form.value;
    this.sessionService.startSession({
      recipeId: this.selectedRecipeId()!,
      targetVolumeL: v.targetVolumeL!,
      boilOffRatePercent: v.boilOffRatePercent ?? undefined,
      waterToGrainRatio: v.waterToGrainRatio ?? undefined,
      notes: v.notes ?? undefined,
    }).subscribe({
      next: session => this.router.navigate(['/brew-mode/session', session.id]),
      error: err => {
        this.starting.set(false);
        this.snackBar.open(err.error?.message ?? 'Failed to start session', 'Dismiss', { duration: 4000 });
      },
    });
  }

  back(): void {
    this.router.navigate(['/recipes']);
  }
}
