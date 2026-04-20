import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RecipeService } from '../recipe.service';
import {
  INGREDIENT_CATEGORIES,
  INGREDIENT_UNITS,
  RECIPE_PHASES,
  SaveRecipeRequest,
} from '../../../core/models/recipe.model';

@Component({
  selector: 'app-recipe-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatSnackBarModule,
  ],
  template: `
    <mat-toolbar color="primary" class="toolbar">
      <button mat-icon-button (click)="cancel()"><mat-icon>arrow_back</mat-icon></button>
      <span class="title">{{ isEdit() ? 'Edit Recipe' : 'New Recipe' }}</span>
      <span class="spacer"></span>
      <button mat-button (click)="submit()" [disabled]="form.invalid || isSubmitting()">
        @if (isSubmitting()) {
          <mat-spinner diameter="18" style="display:inline-block;vertical-align:middle"></mat-spinner>
        } @else {
          Save
        }
      </button>
    </mat-toolbar>

    @if (isLoading()) {
      <div class="center"><mat-spinner></mat-spinner></div>
    } @else {
      <div class="form-container" [formGroup]="form">

        <!-- Basic info -->
        <div class="section-label">Basic Info</div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Recipe Name *</mat-label>
          <input matInput formControlName="name">
          @if (fc('name').invalid && fc('name').touched) {
            <mat-error>Name is required</mat-error>
          }
        </mat-form-field>

        <div class="row-2">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Style</mat-label>
            <input matInput formControlName="style" placeholder="American IPA">
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Base Volume (L) *</mat-label>
            <input matInput type="number" formControlName="baseVolumeL" min="0.1">
            @if (fc('baseVolumeL').invalid && fc('baseVolumeL').touched) {
              <mat-error>Required, min 0.1L</mat-error>
            }
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Source URL</mat-label>
          <input matInput formControlName="sourceUrl" type="url">
        </mat-form-field>

        <mat-divider></mat-divider>

        <!-- Brewing stats -->
        <div class="section-label">Brewing Stats</div>

        <div class="row-3">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>OG</mat-label>
            <input matInput type="number" formControlName="originalGravity" step="0.001" placeholder="1.062">
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>FG</mat-label>
            <input matInput type="number" formControlName="finalGravity" step="0.001" placeholder="1.012">
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>ABV (%)</mat-label>
            <input matInput type="number" formControlName="abv" step="0.1" min="0" max="100">
          </mat-form-field>
        </div>

        <div class="row-3">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>IBU</mat-label>
            <input matInput type="number" formControlName="ibu" min="0">
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>SRM</mat-label>
            <input matInput type="number" formControlName="srm" step="0.1" min="0">
          </mat-form-field>
          <div></div>
        </div>

        <div class="row-3">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Mash Temp (°C)</mat-label>
            <input matInput type="number" formControlName="mashTempC" step="0.5">
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Mash Time (min)</mat-label>
            <input matInput type="number" formControlName="mashDurationMin" min="0">
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Boil Time (min)</mat-label>
            <input matInput type="number" formControlName="boilDurationMin" min="0">
          </mat-form-field>
        </div>

        <div class="row-3">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Ferm Temp (°C)</mat-label>
            <input matInput type="number" formControlName="fermentationTempC" step="0.5">
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Ferm Days</mat-label>
            <input matInput type="number" formControlName="fermentationDays" min="0">
          </mat-form-field>
          <div></div>
        </div>

        <mat-divider></mat-divider>

        <!-- Ingredients -->
        <div class="section-header">
          <div class="section-label">Ingredients</div>
          <button mat-stroked-button color="primary" type="button" (click)="addIngredient()">
            <mat-icon>add</mat-icon> Add
          </button>
        </div>

        <div formArrayName="ingredients">
          @for (row of ingredientsArray.controls; track row; let i = $index) {
            <div [formGroupName]="i" class="array-row">
              <div class="row-3">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Name *</mat-label>
                  <input matInput formControlName="name">
                </mat-form-field>
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Category *</mat-label>
                  <mat-select formControlName="category">
                    @for (cat of ingredientCategories; track cat) {
                      <mat-option [value]="cat">{{ cat }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <button mat-icon-button color="warn" type="button" (click)="removeIngredient(i)" class="remove-btn">
                  <mat-icon>delete</mat-icon>
                </button>
              </div>
              <div class="row-3">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Amount *</mat-label>
                  <input matInput type="number" formControlName="amount" step="0.001" min="0.001">
                </mat-form-field>
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Unit *</mat-label>
                  <mat-select formControlName="unit">
                    @for (u of ingredientUnits; track u) {
                      <mat-option [value]="u">{{ u }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Addition Time</mat-label>
                  <input matInput formControlName="additionTime" placeholder="60 min before end">
                </mat-form-field>
              </div>
            </div>
          }
        </div>

        <mat-divider></mat-divider>

        <!-- Steps -->
        <div class="section-header">
          <div class="section-label">Brew Steps</div>
          <button mat-stroked-button color="primary" type="button" (click)="addStep()">
            <mat-icon>add</mat-icon> Add
          </button>
        </div>

        <div formArrayName="steps">
          @for (row of stepsArray.controls; track row; let i = $index) {
            <div [formGroupName]="i" class="array-row">
              <div class="step-row-header">
                <div class="step-index">Step {{ i + 1 }}</div>
                <mat-form-field appearance="outline" class="phase-select">
                  <mat-label>Phase *</mat-label>
                  <mat-select formControlName="phase">
                    @for (p of recipePhases; track p) {
                      <mat-option [value]="p">{{ p }}</mat-option>
                    }
                  </mat-select>
                </mat-form-field>
                <button mat-icon-button color="warn" type="button" (click)="removeStep(i)" class="remove-btn">
                  <mat-icon>delete</mat-icon>
                </button>
              </div>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Title *</mat-label>
                <input matInput formControlName="title">
              </mat-form-field>
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Instructions *</mat-label>
                <textarea matInput formControlName="instructions" rows="3"></textarea>
              </mat-form-field>
              <div class="row-3">
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Duration (min)</mat-label>
                  <input matInput type="number" formControlName="durationMin" min="0">
                </mat-form-field>
                <mat-form-field appearance="outline" class="full-width">
                  <mat-label>Target Temp (°C)</mat-label>
                  <input matInput type="number" formControlName="targetTempC" step="0.5">
                </mat-form-field>
                <div class="timer-checkbox">
                  <mat-checkbox formControlName="timerRequired" color="primary">Timer required</mat-checkbox>
                </div>
              </div>
            </div>
          }
        </div>

        <mat-divider></mat-divider>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Notes</mat-label>
          <textarea matInput formControlName="notes" rows="3"></textarea>
        </mat-form-field>

      </div>
    }
  `,
  styles: [`
    :host { display: block; background: #1A1A1A; min-height: 100%; }
    .toolbar { position: sticky; top: 0; z-index: 10; }
    .title { font-size: 17px; font-weight: 600; }
    .spacer { flex: 1 1 auto; }
    .center { display: flex; align-items: center; justify-content: center; height: 60vh; }
    .form-container { padding: 16px 16px 80px; display: flex; flex-direction: column; gap: 8px; }
    .full-width { width: 100%; }
    .section-label { font-size: 12px; font-weight: 700; color: rgba(255,255,255,.5); text-transform: uppercase; letter-spacing: .08em; margin: 8px 0 4px; }
    .section-header { display: flex; align-items: center; justify-content: space-between; margin: 8px 0 4px; }
    .row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
    .row-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; }
    .array-row { background: rgba(255,255,255,.03); border-radius: 6px; padding: 10px; margin-bottom: 8px; display: flex; flex-direction: column; gap: 6px; }
    .step-row-header { display: flex; align-items: center; gap: 8px; }
    .step-index { font-weight: 700; font-size: 16px; color: #C17817; min-width: 50px; }
    .phase-select { flex: 1; }
    .remove-btn { flex-shrink: 0; }
    .timer-checkbox { display: flex; align-items: center; }
    @media (max-width: 600px) {
      .row-2 { grid-template-columns: 1fr; }
      .row-3 { grid-template-columns: 1fr; }
    }
  `]
})
export class RecipeFormComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly recipeService = inject(RecipeService);
  private readonly snackBar = inject(MatSnackBar);

  isEdit = signal(false);
  isLoading = signal(false);
  isSubmitting = signal(false);
  private recipeId: string | null = null;

  readonly ingredientCategories = [...INGREDIENT_CATEGORIES];
  readonly ingredientUnits = [...INGREDIENT_UNITS];
  readonly recipePhases = [...RECIPE_PHASES];

  form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    style: new FormControl('', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
    sourceUrl: new FormControl('', { nonNullable: true }),
    baseVolumeL: new FormControl<number | null>(20, [Validators.required, Validators.min(0.1)]),
    originalGravity: new FormControl<number | null>(null),
    finalGravity: new FormControl<number | null>(null),
    abv: new FormControl<number | null>(null),
    ibu: new FormControl<number | null>(null),
    srm: new FormControl<number | null>(null),
    mashTempC: new FormControl<number | null>(null),
    mashDurationMin: new FormControl<number | null>(null),
    boilDurationMin: new FormControl<number | null>(null),
    fermentationTempC: new FormControl<number | null>(null),
    fermentationDays: new FormControl<number | null>(null),
    notes: new FormControl('', { nonNullable: true }),
    ingredients: new FormArray<FormGroup>([]),
    steps: new FormArray<FormGroup>([]),
  });

  get ingredientsArray(): FormArray<FormGroup> {
    return this.form.get('ingredients') as FormArray<FormGroup>;
  }

  get stepsArray(): FormArray<FormGroup> {
    return this.form.get('steps') as FormArray<FormGroup>;
  }

  fc(name: string): AbstractControl {
    return this.form.get(name)!;
  }

  ngOnInit(): void {
    this.recipeId = this.route.snapshot.paramMap.get('id');
    this.isEdit.set(!!this.recipeId);

    if (this.recipeId) {
      this.isLoading.set(true);
      this.recipeService.getRecipe(this.recipeId).subscribe({
        next: recipe => {
          this.form.patchValue({
            name: recipe.name,
            style: recipe.style ?? '',
            description: recipe.description ?? '',
            sourceUrl: recipe.sourceUrl ?? '',
            baseVolumeL: recipe.baseVolumeL,
            originalGravity: recipe.originalGravity,
            finalGravity: recipe.finalGravity,
            abv: recipe.abv,
            ibu: recipe.ibu,
            srm: recipe.srm,
            mashTempC: recipe.mashTempC,
            mashDurationMin: recipe.mashDurationMin,
            boilDurationMin: recipe.boilDurationMin,
            fermentationTempC: recipe.fermentationTempC,
            fermentationDays: recipe.fermentationDays,
            notes: recipe.notes ?? '',
          });
          recipe.ingredients.forEach(i => this.ingredientsArray.push(this.buildIngredientRow(i)));
          recipe.steps.forEach(s => this.stepsArray.push(this.buildStepRow(s)));
          this.isLoading.set(false);
        },
        error: () => {
          this.snackBar.open('Failed to load recipe', 'Close', { duration: 3000 });
          this.isLoading.set(false);
        },
      });
    }
  }

  addIngredient(): void {
    this.ingredientsArray.push(this.buildIngredientRow());
  }

  removeIngredient(index: number): void {
    this.ingredientsArray.removeAt(index);
  }

  addStep(): void {
    const lastPhase = this.stepsArray.length > 0
      ? this.stepsArray.at(this.stepsArray.length - 1).get('phase')?.value
      : undefined;
    this.stepsArray.push(this.buildStepRow(lastPhase ? { phase: lastPhase, title: '', instructions: '' } : undefined));
  }

  removeStep(index: number): void {
    this.stepsArray.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const v = this.form.getRawValue();
    const req: SaveRecipeRequest = {
      name: v.name,
      style: v.style || undefined,
      description: v.description || undefined,
      sourceUrl: v.sourceUrl || undefined,
      baseVolumeL: v.baseVolumeL!,
      originalGravity: v.originalGravity ?? undefined,
      finalGravity: v.finalGravity ?? undefined,
      abv: v.abv ?? undefined,
      ibu: v.ibu ?? undefined,
      srm: v.srm ?? undefined,
      mashTempC: v.mashTempC ?? undefined,
      mashDurationMin: v.mashDurationMin ?? undefined,
      boilDurationMin: v.boilDurationMin ?? undefined,
      fermentationTempC: v.fermentationTempC ?? undefined,
      fermentationDays: v.fermentationDays ?? undefined,
      notes: v.notes || undefined,
      ingredients: v.ingredients.map((i: Record<string, unknown>, idx: number) => ({
        name: i['name'] as string,
        category: i['category'] as string,
        amount: i['amount'] as number,
        unit: i['unit'] as string,
        additionTime: (i['additionTime'] as string) || undefined,
        notes: (i['notes'] as string) || undefined,
        sortOrder: idx,
      })),
      steps: v.steps.map((s: Record<string, unknown>, idx: number) => ({
        stepNumber: idx + 1,
        phase: s['phase'] as string,
        title: s['title'] as string,
        instructions: s['instructions'] as string,
        durationMin: (s['durationMin'] as number) || undefined,
        targetTempC: (s['targetTempC'] as number) || undefined,
        timerRequired: s['timerRequired'] as boolean,
        notes: (s['notes'] as string) || undefined,
      })),
    };

    this.isSubmitting.set(true);

    const op = this.recipeId
      ? this.recipeService.updateRecipe(this.recipeId, req)
      : this.recipeService.createRecipe(req);

    op.subscribe({
      next: r => {
        this.isSubmitting.set(false);
        this.snackBar.open(this.isEdit() ? 'Recipe updated' : 'Recipe created', 'Close', { duration: 2000 });
        this.router.navigate(['/recipes', r.id]);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.snackBar.open('Failed to save recipe', 'Close', { duration: 3000 });
      },
    });
  }

  cancel(): void {
    if (this.recipeId) {
      this.router.navigate(['/recipes', this.recipeId]);
    } else {
      this.router.navigate(['/recipes']);
    }
  }

  private buildIngredientRow(i?: { name: string; category: string; amount: number; unit: string; additionTime?: string | null; notes?: string | null }): FormGroup {
    return new FormGroup({
      name: new FormControl(i?.name ?? '', { nonNullable: true, validators: [Validators.required] }),
      category: new FormControl(i?.category ?? 'MALT', { nonNullable: true, validators: [Validators.required] }),
      amount: new FormControl<number | null>(i?.amount ?? null, [Validators.required, Validators.min(0.001)]),
      unit: new FormControl(i?.unit ?? 'kg', { nonNullable: true, validators: [Validators.required] }),
      additionTime: new FormControl(i?.additionTime ?? '', { nonNullable: true }),
      notes: new FormControl(i?.notes ?? '', { nonNullable: true }),
    });
  }

  private buildStepRow(s?: { phase: string; title: string; instructions: string; durationMin?: number | null; targetTempC?: number | null; timerRequired?: boolean; notes?: string | null }): FormGroup {
    return new FormGroup({
      phase: new FormControl(s?.phase ?? 'MASHING', { nonNullable: true, validators: [Validators.required] }),
      title: new FormControl(s?.title ?? '', { nonNullable: true, validators: [Validators.required] }),
      instructions: new FormControl(s?.instructions ?? '', { nonNullable: true, validators: [Validators.required] }),
      durationMin: new FormControl<number | null>(s?.durationMin ?? null),
      targetTempC: new FormControl<number | null>(s?.targetTempC ?? null),
      timerRequired: new FormControl(s?.timerRequired ?? false, { nonNullable: true }),
      notes: new FormControl(s?.notes ?? '', { nonNullable: true }),
    });
  }
}
