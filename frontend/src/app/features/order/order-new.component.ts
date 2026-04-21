import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
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
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { DecimalPipe } from '@angular/common';
import { RecipeService } from '../recipes/recipe.service';
import { OrderService } from './order.service';
import { RecipeSummary, RecipeDetail } from '../../core/models/recipe.model';
import { OrderResultDto, shopSearchUrl } from '../../core/models/order.model';

type View = 'setup' | 'loading' | 'result';

@Component({
  selector: 'app-order-new',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink,
    MatToolbarModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatAutocompleteModule,
    MatProgressSpinnerModule, MatCardModule, MatSnackBarModule,
    MatDividerModule, MatChipsModule,
    DecimalPipe,
  ],
  template: `
    <mat-toolbar color="primary">
      @if (view() === 'result') {
        <button mat-icon-button (click)="view.set('setup')"><mat-icon>arrow_back</mat-icon></button>
        <span>Order List</span>
        <span class="spacer"></span>
        <button mat-icon-button routerLink="/order/history" title="Order history">
          <mat-icon>history</mat-icon>
        </button>
      } @else {
        <button mat-icon-button (click)="back()"><mat-icon>arrow_back</mat-icon></button>
        <span>New Order List</span>
      }
    </mat-toolbar>

    @if (view() === 'setup') {
      <div class="container">
        <form [formGroup]="form" (ngSubmit)="generate()">
          <mat-card class="form-card">
            <mat-card-header><mat-card-title>Setup</mat-card-title></mat-card-header>
            <mat-card-content>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Recipe</mat-label>
                <input matInput formControlName="recipeName" [matAutocomplete]="auto"
                       placeholder="Search your recipes...">
                <mat-autocomplete #auto="matAutocomplete" [displayWith]="displayRecipe"
                                  (optionSelected)="onRecipeSelected($event.option.value)">
                  @for (r of filteredRecipes(); track r.id) {
                    <mat-option [value]="r">{{ r.name }}
                      @if (r.style) { <span class="style-hint"> · {{ r.style }}</span> }
                    </mat-option>
                  }
                </mat-autocomplete>
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Batch Volume (L)</mat-label>
                <input matInput type="number" formControlName="volumeL" min="1">
                <mat-error>Minimum 1 litre</mat-error>
              </mat-form-field>

              @if (loadingRecipe()) {
                <div class="preview-loading">
                  <mat-spinner diameter="16"></mat-spinner>
                  <span>Loading recipe…</span>
                </div>
              } @else if (previewIngredients()) {
                <p class="preview-label">Scaled ingredients for {{ volumeSignal() }}L</p>
                <div class="preview-table">
                  @for (ing of previewIngredients()!; track ing.name) {
                    <div class="preview-row">
                      <span class="preview-name">{{ ing.name }}</span>
                      <span class="preview-amount">{{ ing.amount | number:'1.0-3' }} {{ ing.unit }}</span>
                    </div>
                  }
                </div>
              }

            </mat-card-content>
          </mat-card>

          <button mat-raised-button color="primary" type="submit"
                  class="generate-btn"
                  [disabled]="form.invalid || !selectedRecipeId()">
            <mat-icon>search</mat-icon> Find Best Prices
          </button>
        </form>
      </div>
    }

    @if (view() === 'loading') {
      <div class="loading-screen">
        <mat-spinner diameter="56"></mat-spinner>
        <p class="loading-text">Searching German homebrew shops for the best prices…</p>
        <p class="loading-sub">This may take up to 30 seconds</p>
      </div>
    }

    @if (view() === 'result' && result()) {
      @if (hasMissingPrices()) {
        <div class="warning-banner">
          <mat-icon>warning</mat-icon>
          <span>{{ missingCount() }} ingredient{{ missingCount() > 1 ? 's' : '' }} could not be priced — verify manually</span>
        </div>
      }

      <div class="container result-container">
        <mat-card class="summary-card">
          <mat-card-content>
            <div class="summary-row">
              <span class="recipe-label">{{ result()!.recipeName }}</span>
              <span class="volume-label">{{ result()!.volumeL }}L</span>
            </div>
            <div class="total-row">
              <mat-icon>euro</mat-icon>
              <span class="total-label">
                {{ result()!.estimatedTotalMin | number:'1.2-2' }} –
                {{ result()!.estimatedTotalMax | number:'1.2-2' }} EUR
              </span>
            </div>
            <p class="disclaimer">{{ result()!.disclaimer }}</p>
          </mat-card-content>
        </mat-card>

        @for (item of result()!.items; track item.ingredientName) {
          <mat-card class="item-card">
            <mat-card-content>
              <div class="item-header">
                <span class="ingredient-name">{{ item.ingredientName }}</span>
                <mat-chip class="amount-chip">{{ item.requiredAmount | number:'1.0-3' }} {{ item.unit }}</mat-chip>
              </div>

              @if (item.bestOffer) {
                @if (item.searchNote) {
                  <p class="search-note">{{ item.searchNote }}</p>
                }
                <div class="offer best-offer">
                  <div class="offer-header">
                    <mat-icon class="offer-icon best">star</mat-icon>
                    <span class="shop-name">{{ item.bestOffer.shopName }}</span>
                    <span class="offer-price">{{ item.bestOffer.totalCost | number:'1.2-2' }} EUR</span>
                  </div>
                  <div class="offer-detail">
                    {{ item.bestOffer.packagesNeeded }}× {{ item.bestOffer.packageSize }} ·
                    {{ item.bestOffer.pricePerUnit }}
                  </div>
                  <a [href]="searchUrl(item.bestOffer.shopDomain, item.ingredientName)" target="_blank" rel="noopener" class="shop-link">
                    <mat-icon>search</mat-icon> Search on {{ item.bestOffer.shopName }}
                  </a>
                </div>
              } @else {
                <div class="no-offer">
                  <mat-icon class="warn-icon">warning</mat-icon>
                  <span>{{ item.searchNote ?? 'No price found — search manually' }}</span>
                </div>
              }

              @if (item.alternativeOffer) {
                <mat-divider></mat-divider>
                <div class="offer alt-offer">
                  <div class="offer-header">
                    <mat-icon class="offer-icon alt">compare_arrows</mat-icon>
                    <span class="shop-name">{{ item.alternativeOffer.shopName }}</span>
                    <span class="offer-price">{{ item.alternativeOffer.price | number:'1.2-2' }} EUR</span>
                  </div>
                  <a [href]="searchUrl(item.alternativeOffer.shopDomain, item.ingredientName)" target="_blank" rel="noopener" class="shop-link">
                    <mat-icon>search</mat-icon> Search on {{ item.alternativeOffer.shopName }}
                  </a>
                </div>
              }
            </mat-card-content>
          </mat-card>
        }
      </div>

      <div class="sticky-footer">
        <div class="footer-total">
          <mat-icon>euro</mat-icon>
          <span>{{ result()!.estimatedTotalMin | number:'1.2-2' }} – {{ result()!.estimatedTotalMax | number:'1.2-2' }} EUR</span>
        </div>
        <button mat-raised-button color="primary" (click)="copyShoppingList()">
          <mat-icon>content_copy</mat-icon> Copy List
        </button>
      </div>
    }
  `,
  styles: [`
    :host { display: flex; flex-direction: column; height: 100%; background: #1a1a1a; }
    .spacer { flex: 1; }
    .container { padding: 16px; overflow-y: auto; flex: 1; }
    .result-container { padding-bottom: 8px; }
    .form-card, .summary-card, .item-card {
      background: #2a2a2a !important;
      color: rgba(255,255,255,.87) !important;
      margin-bottom: 16px;
    }
    mat-card-title { color: rgba(255,255,255,.87) !important; }
    .full-width { width: 100%; margin-bottom: 8px; }
    .generate-btn { width: 100%; margin-top: 8px; height: 48px; }
    .style-hint { color: rgba(255,255,255,.45); font-size: 12px; }

    /* dark form field overrides */
    :host ::ng-deep .mdc-text-field--outlined .mdc-notched-outline__leading,
    :host ::ng-deep .mdc-text-field--outlined .mdc-notched-outline__notch,
    :host ::ng-deep .mdc-text-field--outlined .mdc-notched-outline__trailing {
      border-color: rgba(255,255,255,.3) !important;
    }
    :host ::ng-deep .mdc-text-field--outlined:not(.mdc-text-field--disabled) .mdc-text-field__input {
      color: rgba(255,255,255,.9) !important; caret-color: white;
    }
    :host ::ng-deep .mat-mdc-form-field .mdc-floating-label { color: rgba(255,255,255,.6) !important; }
    :host ::ng-deep .mdc-text-field--outlined:not(.mdc-text-field--disabled) {
      background: rgba(255,255,255,.05) !important;
    }
    :host ::ng-deep .mat-mdc-autocomplete-panel { background: #2a2a2a; }
    :host ::ng-deep .mat-mdc-option { color: rgba(255,255,255,.87); }
    :host ::ng-deep .mat-mdc-raised-button:disabled {
      background: rgba(255,255,255,.12) !important;
      color: rgba(255,255,255,.38) !important;
    }

    /* ingredient preview */
    .preview-loading { display: flex; align-items: center; gap: 8px; color: rgba(255,255,255,.5); font-size: 13px; padding: 6px 0; }
    .preview-label { font-size: 12px; color: rgba(255,255,255,.45); margin: 8px 0 4px; }
    .preview-table { background: rgba(255,255,255,.04); border-radius: 4px; overflow: hidden; }
    .preview-row {
      display: flex; justify-content: space-between; align-items: center;
      padding: 6px 8px; font-size: 13px;
      border-bottom: 1px solid rgba(255,255,255,.06);
    }
    .preview-row:last-child { border-bottom: none; }
    .preview-name { color: rgba(255,255,255,.7); }
    .preview-amount { color: rgba(255,255,255,.5); font-size: 12px; }

    /* loading */
    .loading-screen {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 16px;
      color: rgba(255,255,255,.87);
    }
    .loading-text { font-size: 16px; margin: 0; }
    .loading-sub { font-size: 13px; color: rgba(255,255,255,.45); margin: 0; }

    /* warning banner */
    .warning-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 16px;
      background: rgba(255, 152, 0, .12);
      border-bottom: 1px solid rgba(255, 152, 0, .3);
      color: #ffa726;
      font-size: 13px;
      flex-shrink: 0;
    }
    .warning-banner mat-icon { font-size: 18px; width: 18px; height: 18px; }

    /* result */
    .summary-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .recipe-label { font-size: 16px; font-weight: 500; color: rgba(255,255,255,.87); }
    .volume-label { font-size: 14px; color: rgba(255,255,255,.6); }
    .total-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
    .total-row mat-icon { color: #ffa726; }
    .total-label { font-size: 18px; font-weight: 600; color: rgba(255,255,255,.87); }
    .disclaimer { font-size: 11px; color: rgba(255,255,255,.4); margin: 0; }

    .item-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .ingredient-name { font-size: 15px; font-weight: 500; color: rgba(255,255,255,.87); }
    .amount-chip { background: rgba(255,255,255,.1) !important; color: rgba(255,255,255,.7) !important; font-size: 12px !important; }
    .search-note { font-size: 12px; color: rgba(255,255,255,.5); margin: 4px 0 8px; }

    .offer { padding: 8px 0; }
    .offer-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
    .offer-icon { font-size: 18px; width: 18px; height: 18px; }
    .offer-icon.best { color: #ffa726; }
    .offer-icon.alt { color: rgba(255,255,255,.5); }
    .shop-name { flex: 1; font-size: 13px; color: rgba(255,255,255,.7); }
    .offer-price { font-size: 14px; font-weight: 600; color: rgba(255,255,255,.87); }
    .offer-detail { font-size: 12px; color: rgba(255,255,255,.5); margin-bottom: 4px; padding-left: 26px; }
    .shop-link { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #C17817; text-decoration: none; padding-left: 26px; }
    .shop-link mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .no-offer {
      display: flex; align-items: center; gap: 6px;
      font-size: 13px; color: rgba(255,255,255,.5); padding: 6px 0;
    }
    .warn-icon { font-size: 16px; width: 16px; height: 16px; color: #ffa726; }
    mat-divider { margin: 8px 0; border-color: rgba(255,255,255,.1) !important; }

    /* sticky footer */
    .sticky-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      background: #2a2a2a;
      border-top: 1px solid rgba(255,255,255,.1);
      flex-shrink: 0;
    }
    .footer-total { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; color: rgba(255,255,255,.87); }
    .footer-total mat-icon { color: #ffa726; font-size: 18px; width: 18px; height: 18px; }
  `]
})
export class OrderNewComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly recipeService = inject(RecipeService);
  private readonly orderService = inject(OrderService);
  private readonly snackBar = inject(MatSnackBar);

  readonly view = signal<View>('setup');
  readonly result = signal<OrderResultDto | null>(null);
  readonly selectedRecipeId = signal<string | null>(null);
  readonly selectedRecipe = signal<RecipeDetail | null>(null);
  readonly loadingRecipe = signal(false);
  readonly recipes = signal<RecipeSummary[]>([]);
  readonly loadingRecipes = signal(true);
  readonly volumeSignal = signal<number>(20);

  readonly form = this.fb.group({
    recipeName: ['', Validators.required],
    volumeL: [20, [Validators.required, Validators.min(1)]],
  });

  readonly filteredRecipes = computed(() => {
    const query = this.form.get('recipeName')?.value?.toLowerCase() ?? '';
    return this.recipes().filter(r =>
      r.name.toLowerCase().includes(query) || r.style?.toLowerCase().includes(query)
    );
  });

  readonly previewIngredients = computed(() => {
    const recipe = this.selectedRecipe();
    const vol = this.volumeSignal();
    if (!recipe || vol < 1) return null;
    const scale = vol / recipe.baseVolumeL;
    return recipe.ingredients
      .slice()
      .sort((a, b) => a.sortOrder - b.sortOrder)
      .map(ing => ({
        name: ing.name,
        amount: Math.round(ing.amount * scale * 1000) / 1000,
        unit: ing.unit,
      }));
  });

  readonly missingCount = computed(() => this.result()?.items.filter(i => !i.bestOffer).length ?? 0);
  readonly hasMissingPrices = computed(() => this.missingCount() > 0);

  ngOnInit() {
    this.recipeService.getRecipes(0, 200).subscribe({
      next: page => {
        this.recipes.set(page.content);
        this.loadingRecipes.set(false);
      },
      error: () => this.loadingRecipes.set(false)
    });
    this.form.get('volumeL')!.valueChanges.subscribe(val => {
      if (val != null && val >= 1) this.volumeSignal.set(val);
    });
  }

  displayRecipe = (r: RecipeSummary | string | null): string => {
    if (!r) return '';
    if (typeof r === 'string') return r;
    return r.name;
  };

  onRecipeSelected(r: RecipeSummary) {
    this.selectedRecipeId.set(r.id);
    this.selectedRecipe.set(null);
    this.loadingRecipe.set(true);
    this.recipeService.getRecipe(r.id).subscribe({
      next: detail => {
        this.selectedRecipe.set(detail);
        this.loadingRecipe.set(false);
      },
      error: () => this.loadingRecipe.set(false)
    });
  }

  generate() {
    if (this.form.invalid || !this.selectedRecipeId()) return;
    this.view.set('loading');
    this.orderService.generate({
      recipeId: this.selectedRecipeId()!,
      volumeL: this.form.value.volumeL!
    }).subscribe({
      next: result => {
        this.result.set(result);
        this.view.set('result');
      },
      error: err => {
        this.view.set('setup');
        const msg = err?.error?.message ?? 'Failed to generate order list. Please try again.';
        this.snackBar.open(msg, 'Dismiss', { duration: 5000 });
      }
    });
  }

  copyShoppingList() {
    const r = this.result();
    if (!r) return;
    const lines: string[] = [
      `Order List: ${r.recipeName} (${r.volumeL}L)`,
      `Generated: ${new Date(r.generatedAt).toLocaleDateString()}`,
      '',
    ];
    for (const item of r.items) {
      lines.push(`${item.ingredientName} - ${item.requiredAmount} ${item.unit}`);
      if (item.bestOffer) {
        const url = shopSearchUrl(item.bestOffer.shopDomain, item.ingredientName);
        lines.push(`  Best: ${item.bestOffer.shopName} - €${item.bestOffer.totalCost.toFixed(2)} - ${url}`);
      } else {
        lines.push(`  No price found${item.searchNote ? ` (${item.searchNote})` : ''}`);
      }
      if (item.alternativeOffer) {
        const url = shopSearchUrl(item.alternativeOffer.shopDomain, item.ingredientName);
        lines.push(`  Alt: ${item.alternativeOffer.shopName} - €${item.alternativeOffer.price.toFixed(2)} - ${url}`);
      }
      lines.push('');
    }
    lines.push(`Total: €${r.estimatedTotalMin.toFixed(2)} – €${r.estimatedTotalMax.toFixed(2)}`);
    lines.push(r.disclaimer);

    navigator.clipboard.writeText(lines.join('\n')).then(
      () => this.snackBar.open('Copied to clipboard!', undefined, { duration: 2000 }),
      () => this.snackBar.open('Copy failed — try again', undefined, { duration: 2000 })
    );
  }

  searchUrl(shopDomain: string, ingredient: string): string {
    return shopSearchUrl(shopDomain, ingredient);
  }

  back() {
    this.router.navigate(['/calendar']);
  }
}
