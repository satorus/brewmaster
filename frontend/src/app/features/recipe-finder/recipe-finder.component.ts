import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSliderModule } from '@angular/material/slider';
import { MatChipsModule, MatChipListboxChange } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatDividerModule } from '@angular/material/divider';
import { RecipeService } from '../recipes/recipe.service';
import {
  AiRecipeDto,
  AROMA_NOTES,
  BEER_COLOURS,
  BEER_STYLES,
  SaveRecipeRequest,
  TasteProfileRequest,
} from '../../core/models/recipe.model';

type ViewState = 'form' | 'loading' | 'results' | 'error';

@Component({
  selector: 'app-recipe-finder',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSliderModule,
    MatChipsModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDialogModule,
    MatDividerModule,
  ],
  template: `
    <mat-toolbar color="primary" class="toolbar">
      <mat-icon>search</mat-icon>
      <span class="title">Recipe Finder</span>
    </mat-toolbar>

    <!-- FORM -->
    @if (viewState() === 'form') {
      <div class="page-content">
        <form [formGroup]="form" class="taste-form">

          <mat-form-field appearance="fill" class="full-width">
            <mat-label>Beer Style</mat-label>
            <mat-select formControlName="style">
              @for (s of beerStyles; track s) {
                <mat-option [value]="s">{{ s }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <div class="slider-field">
            <div class="slider-label-row">
              <span class="slider-label">Bitterness</span>
              <span class="slider-value">{{ bitternessLabel(form.get('bitternessLevel')!.value) }}</span>
            </div>
            <mat-slider min="1" max="5" step="1" discrete class="full-slider">
              <input matSliderThumb formControlName="bitternessLevel">
            </mat-slider>
            <div class="slider-endpoints"><span>Very Low</span><span>Very High</span></div>
          </div>

          <div class="slider-field">
            <div class="slider-label-row">
              <span class="slider-label">Sweetness</span>
              <span class="slider-value">{{ sweetnessLabel(form.get('sweetnessLevel')!.value) }}</span>
            </div>
            <mat-slider min="1" max="5" step="1" discrete class="full-slider">
              <input matSliderThumb formControlName="sweetnessLevel">
            </mat-slider>
            <div class="slider-endpoints"><span>Very Dry</span><span>Very Sweet</span></div>
          </div>

          <mat-form-field appearance="fill" class="full-width">
            <mat-label>Colour</mat-label>
            <mat-select formControlName="colour">
              @for (c of beerColours; track c) {
                <mat-option [value]="c">{{ c }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <div class="slider-field">
            <div class="slider-label-row">
              <span class="slider-label">Target ABV</span>
              <span class="slider-value">
                {{ form.get('targetAbvMin')!.value }}% – {{ form.get('targetAbvMax')!.value }}%
              </span>
            </div>
            <mat-slider min="0" max="12" step="0.5" class="full-slider">
              <input matSliderStartThumb formControlName="targetAbvMin">
              <input matSliderEndThumb formControlName="targetAbvMax">
            </mat-slider>
            <div class="slider-endpoints"><span>0%</span><span>12%</span></div>
          </div>

          <div class="aroma-field">
            <span class="field-label">Aroma Notes</span>
            <mat-chip-listbox multiple (change)="onAromaChange($event)" class="aroma-chips">
              @for (note of aromaOptions; track note) {
                <mat-chip-option [value]="note">{{ note }}</mat-chip-option>
              }
            </mat-chip-listbox>
          </div>

          <mat-form-field appearance="fill" class="full-width">
            <mat-label>Batch Volume (litres)</mat-label>
            <input matInput type="number" formControlName="batchVolumeL" min="1" max="1000">
            <mat-error>Enter a volume of at least 1 litre</mat-error>
          </mat-form-field>

          <mat-form-field appearance="fill" class="full-width">
            <mat-label>Additional Notes (optional)</mat-label>
            <textarea matInput formControlName="additionalNotes" rows="3"
              placeholder="e.g. suitable for summer, low alcohol preferred..."></textarea>
          </mat-form-field>

          <button mat-raised-button color="primary" class="search-btn"
            [disabled]="form.invalid"
            (click)="search()">
            <mat-icon>auto_awesome</mat-icon>
            Find Recipes
          </button>
        </form>
      </div>
    }

    <!-- LOADING -->
    @if (viewState() === 'loading') {
      <div class="center-state">
        <mat-spinner diameter="56"></mat-spinner>
        <p class="loading-primary">Searching the web for recipes...</p>
        <p class="loading-secondary">This can take up to 60 seconds while the AI searches homebrew databases.</p>
      </div>
    }

    <!-- ERROR -->
    @if (viewState() === 'error') {
      <div class="center-state">
        <mat-icon class="error-icon">error_outline</mat-icon>
        <p class="error-primary">{{ errorMessage() }}</p>
        <button mat-raised-button color="primary" (click)="viewState.set('form')">
          <mat-icon>arrow_back</mat-icon> Try Again
        </button>
      </div>
    }

    <!-- RESULTS -->
    @if (viewState() === 'results') {
      <div class="page-content">
        <div class="results-header">
          <span class="results-count">{{ aiResults().length }} recipes found</span>
          <button mat-button (click)="viewState.set('form')">
            <mat-icon>tune</mat-icon> New Search
          </button>
        </div>

        @for (recipe of aiResults(); track recipe.name; let i = $index) {
          <mat-card class="result-card">
            <mat-card-header>
              <mat-card-title>{{ recipe.name }}</mat-card-title>
              <mat-card-subtitle>{{ recipe.style }}</mat-card-subtitle>
            </mat-card-header>

            <mat-card-content>
              @if (recipe.description) {
                <p class="recipe-desc">{{ recipe.description }}</p>
              }

              <div class="stats-row">
                @if (recipe.abv != null) {
                  <div class="stat"><span class="stat-l">ABV</span><span class="stat-v">{{ recipe.abv }}%</span></div>
                }
                @if (recipe.ibu != null) {
                  <div class="stat"><span class="stat-l">IBU</span><span class="stat-v">{{ recipe.ibu }}</span></div>
                }
                @if (recipe.srm != null) {
                  <div class="stat"><span class="stat-l">SRM</span><span class="stat-v">{{ recipe.srm }}</span></div>
                }
                @if (recipe.originalGravity != null) {
                  <div class="stat"><span class="stat-l">OG</span><span class="stat-v">{{ recipe.originalGravity }}</span></div>
                }
                @if (recipe.finalGravity != null) {
                  <div class="stat"><span class="stat-l">FG</span><span class="stat-v">{{ recipe.finalGravity }}</span></div>
                }
                <div class="stat">
                  <span class="stat-l">Vol</span>
                  <span class="stat-v">{{ recipe.baseVolumeL }}L</span>
                </div>
              </div>

              <div class="meta-row">
                <mat-icon class="meta-icon">kitchen</mat-icon>
                <span>{{ recipe.ingredients.length }} ingredients</span>
                <span class="meta-sep">·</span>
                <mat-icon class="meta-icon">format_list_numbered</mat-icon>
                <span>{{ recipe.steps.length }} steps</span>
              </div>

              @if (recipe.sourceUrl) {
                <a [href]="recipe.sourceUrl" target="_blank" rel="noopener noreferrer" class="source-link">
                  <mat-icon class="link-icon">open_in_new</mat-icon>
                  View original recipe
                </a>
              }
            </mat-card-content>

            <mat-divider></mat-divider>

            <mat-card-actions>
              @if (savedRecipeIds().has(i)) {
                <div class="saved-actions">
                  <span class="saved-badge"><mat-icon>check_circle</mat-icon> Saved!</span>
                  <button mat-button color="primary" (click)="goToRecipes()">View in My Recipes</button>
                  <button mat-button disabled title="Coming soon">Go to Brew Mode</button>
                </div>
              } @else {
                <button mat-raised-button color="primary"
                  [disabled]="savingIndex() === i"
                  (click)="saveRecipe(recipe, i)">
                  @if (savingIndex() === i) {
                    <mat-spinner diameter="18" class="btn-spinner"></mat-spinner>
                    Saving...
                  } @else {
                    <mat-icon>save</mat-icon>
                    Save &amp; Use This Recipe
                  }
                </button>
              }
            </mat-card-actions>
          </mat-card>
        }
      </div>
    }
  `,
  styles: [`
    :host { display: block; background: #1A1A1A; min-height: 100%; }
    .toolbar { position: sticky; top: 0; z-index: 10; gap: 8px; }
    .title { font-size: 18px; font-weight: 600; }
    .page-content { padding: 16px 16px 80px; display: flex; flex-direction: column; gap: 16px; }

    /* Form */
    .taste-form { display: flex; flex-direction: column; gap: 16px; }
    .full-width { width: 100%; }

    .slider-field { display: flex; flex-direction: column; background: #2C2C2C; border-radius: 8px; padding: 12px 16px 4px; }
    .slider-label-row { display: flex; justify-content: space-between; align-items: baseline; }
    .slider-label { font-size: 12px; color: rgba(255,255,255,.5); text-transform: uppercase; letter-spacing: .08em; }
    .slider-value { font-size: 14px; font-weight: 600; color: #C17817; }
    .full-slider { width: 100%; }
    .slider-endpoints { display: flex; justify-content: space-between; font-size: 10px; color: rgba(255,255,255,.3); margin-top: -4px; padding: 0 2px; }

    .aroma-field { background: #2C2C2C; border-radius: 8px; padding: 12px 16px; display: flex; flex-direction: column; gap: 10px; }
    .field-label { font-size: 12px; color: rgba(255,255,255,.5); text-transform: uppercase; letter-spacing: .08em; }
    .aroma-chips { display: flex; flex-wrap: wrap; gap: 4px; }

    .search-btn { height: 48px; font-size: 16px; display: flex; align-items: center; gap: 8px; }
    .search-btn mat-icon { margin-right: 4px; }

    /* Loading / Error */
    .center-state { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 60vh; gap: 16px; padding: 24px; text-align: center; }
    .loading-primary { font-size: 18px; font-weight: 600; color: #fff; margin: 0; }
    .loading-secondary { font-size: 13px; color: rgba(255,255,255,.45); margin: 0; max-width: 280px; }
    .error-icon { font-size: 48px; width: 48px; height: 48px; color: #F44336; }
    .error-primary { font-size: 16px; color: rgba(255,255,255,.8); margin: 0; }

    /* Results */
    .results-header { display: flex; align-items: center; justify-content: space-between; }
    .results-count { font-size: 13px; color: rgba(255,255,255,.5); }

    .result-card { background: #2C2C2C !important; }
    .recipe-desc { font-size: 13px; color: rgba(255,255,255,.7); line-height: 1.5; margin: 8px 0; }

    .stats-row { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0; }
    .stat { display: flex; flex-direction: column; align-items: center; background: rgba(255,255,255,.05); border-radius: 6px; padding: 6px 10px; min-width: 52px; }
    .stat-l { font-size: 10px; color: rgba(255,255,255,.4); text-transform: uppercase; letter-spacing: .06em; }
    .stat-v { font-size: 13px; font-weight: 600; color: #C17817; }

    .meta-row { display: flex; align-items: center; gap: 6px; font-size: 12px; color: rgba(255,255,255,.45); margin: 8px 0; }
    .meta-icon { font-size: 14px; width: 14px; height: 14px; }
    .meta-sep { color: rgba(255,255,255,.2); }

    .source-link { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #C17817; text-decoration: none; margin-top: 4px; }
    .link-icon { font-size: 14px; width: 14px; height: 14px; }

    .saved-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; padding: 4px 0; }
    .saved-badge { display: flex; align-items: center; gap: 4px; font-size: 13px; font-weight: 600; color: #4CAF50; padding: 0 8px; }
    .saved-badge mat-icon { font-size: 16px; width: 16px; height: 16px; }

    .btn-spinner { display: inline-block; margin-right: 8px; }
    mat-card-actions { padding: 8px 16px 12px; }
  `]
})
export class RecipeFinderComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly recipeService = inject(RecipeService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  readonly beerStyles = [...BEER_STYLES];
  readonly beerColours = [...BEER_COLOURS];
  readonly aromaOptions = [...AROMA_NOTES];

  viewState = signal<ViewState>('form');
  aiResults = signal<AiRecipeDto[]>([]);
  errorMessage = signal('');
  savingIndex = signal<number | null>(null);
  savedRecipeIds = signal<Set<number>>(new Set());
  selectedAromas = signal<string[]>([]);

  form!: FormGroup;

  ngOnInit(): void {
    this.form = this.fb.group({
      style: ['IPA'],
      bitternessLevel: [3],
      sweetnessLevel: [3],
      colour: ['Pale'],
      targetAbvMin: [4],
      targetAbvMax: [7],
      batchVolumeL: [20, [Validators.required, Validators.min(1)]],
      additionalNotes: [''],
    });
  }

  onAromaChange(event: MatChipListboxChange): void {
    this.selectedAromas.set(event.value ?? []);
  }

  search(): void {
    if (this.form.invalid) return;

    const v = this.form.value;
    const req: TasteProfileRequest = {
      style: v.style,
      bitternessLevel: v.bitternessLevel,
      sweetnessLevel: v.sweetnessLevel,
      colour: v.colour,
      targetAbvMin: v.targetAbvMin,
      targetAbvMax: v.targetAbvMax,
      aromaNotes: this.selectedAromas(),
      batchVolumeL: v.batchVolumeL,
      additionalNotes: v.additionalNotes || undefined,
    };

    this.viewState.set('loading');
    this.aiResults.set([]);
    this.savedRecipeIds.set(new Set());

    this.recipeService.aiSearchRecipes(req).subscribe({
      next: (res) => {
        this.aiResults.set(res.recipes);
        this.viewState.set('results');
      },
      error: (err) => {
        const msg = err?.error?.message ?? 'Failed to search for recipes. Please try again.';
        this.errorMessage.set(msg);
        this.viewState.set('error');
      },
    });
  }

  saveRecipe(recipe: AiRecipeDto, index: number): void {
    this.savingIndex.set(index);
    const req = this.toSaveRequest(recipe);

    this.recipeService.createRecipe(req).subscribe({
      next: (saved) => {
        this.savingIndex.set(null);
        this.snackBar.open('Recipe saved successfully!', 'Close', { duration: 3000 });
        const ids = new Set(this.savedRecipeIds());
        ids.add(index);
        this.savedRecipeIds.set(ids);
      },
      error: () => {
        this.savingIndex.set(null);
        this.snackBar.open('Failed to save recipe. Please try again.', 'Close', { duration: 3000 });
      },
    });
  }

  goToRecipes(): void {
    this.router.navigate(['/recipes']);
  }

  bitternessLabel(level: number): string {
    const labels = ['', 'Very Low', 'Low', 'Medium', 'High', 'Very High'];
    return labels[level] ?? 'Medium';
  }

  sweetnessLabel(level: number): string {
    const labels = ['', 'Very Dry', 'Dry', 'Medium', 'Sweet', 'Very Sweet'];
    return labels[level] ?? 'Medium';
  }

  private toSaveRequest(recipe: AiRecipeDto): SaveRecipeRequest {
    return {
      name: recipe.name,
      style: recipe.style ?? undefined,
      description: recipe.description ?? undefined,
      sourceUrl: recipe.sourceUrl ?? undefined,
      baseVolumeL: recipe.baseVolumeL,
      originalGravity: recipe.originalGravity ?? undefined,
      finalGravity: recipe.finalGravity ?? undefined,
      abv: recipe.abv ?? undefined,
      ibu: recipe.ibu ?? undefined,
      srm: recipe.srm ?? undefined,
      mashTempC: recipe.mashTempC ?? undefined,
      mashDurationMin: recipe.mashDurationMin ?? undefined,
      boilDurationMin: recipe.boilDurationMin ?? undefined,
      fermentationTempC: recipe.fermentationTempC ?? undefined,
      fermentationDays: recipe.fermentationDays ?? undefined,
      ingredients: recipe.ingredients.map(i => ({
        name: i.name,
        category: i.category,
        amount: i.amount,
        unit: i.unit,
        additionTime: i.additionTime ?? undefined,
        notes: i.notes ?? undefined,
        sortOrder: i.sortOrder,
      })),
      steps: recipe.steps.map(s => ({
        stepNumber: s.stepNumber,
        phase: s.phase,
        title: s.title,
        instructions: s.instructions,
        durationMin: s.durationMin ?? undefined,
        targetTempC: s.targetTempC ?? undefined,
        timerRequired: s.timerRequired,
        notes: s.notes ?? undefined,
      })),
      aiGenerated: true,
    };
  }
}
