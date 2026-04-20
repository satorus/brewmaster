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
import { DecimalPipe, CurrencyPipe, AsyncPipe } from '@angular/common';
import { map, startWith } from 'rxjs/operators';
import { RecipeService } from '../recipes/recipe.service';
import { OrderService } from './order.service';
import { RecipeSummary } from '../../core/models/recipe.model';
import { OrderItemDto, OrderResultDto } from '../../core/models/order.model';

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
    DecimalPipe, CurrencyPipe, AsyncPipe,
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
        <p class="loading-text">Searching shops for best prices…</p>
        <p class="loading-sub">This may take up to 30 seconds</p>
      </div>
    }

    @if (view() === 'result' && result()) {
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

              @if (item.searchNote) {
                <p class="search-note">{{ item.searchNote }}</p>
              }

              @if (item.bestOffer) {
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
                  <a [href]="item.bestOffer.productUrl" target="_blank" rel="noopener" class="shop-link">
                    <mat-icon>open_in_new</mat-icon> View product
                  </a>
                </div>
              } @else {
                <div class="no-offer">No price found — search manually</div>
              }

              @if (item.alternativeOffer) {
                <mat-divider></mat-divider>
                <div class="offer alt-offer">
                  <div class="offer-header">
                    <mat-icon class="offer-icon alt">compare_arrows</mat-icon>
                    <span class="shop-name">{{ item.alternativeOffer.shopName }}</span>
                    <span class="offer-price">{{ item.alternativeOffer.price | number:'1.2-2' }} EUR</span>
                  </div>
                  <a [href]="item.alternativeOffer.productUrl" target="_blank" rel="noopener" class="shop-link">
                    <mat-icon>open_in_new</mat-icon> View product
                  </a>
                </div>
              }
            </mat-card-content>
          </mat-card>
        }
      </div>
    }
  `,
  styles: [`
    :host { display: flex; flex-direction: column; height: 100%; background: #1a1a1a; }
    .spacer { flex: 1; }
    .container { padding: 16px; overflow-y: auto; flex: 1; }
    .result-container { padding-bottom: 24px; }
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
    .no-offer { font-size: 13px; color: rgba(255,255,255,.4); padding: 8px 0; }
    mat-divider { margin: 8px 0; border-color: rgba(255,255,255,.1) !important; }
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
  readonly recipes = signal<RecipeSummary[]>([]);
  readonly loadingRecipes = signal(true);

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

  ngOnInit() {
    this.recipeService.getRecipes(0, 200).subscribe({
      next: page => {
        this.recipes.set(page.content);
        this.loadingRecipes.set(false);
      },
      error: () => this.loadingRecipes.set(false)
    });
  }

  displayRecipe = (r: RecipeSummary | string | null): string => {
    if (!r) return '';
    if (typeof r === 'string') return r;
    return r.name;
  };

  onRecipeSelected(r: RecipeSummary) {
    this.selectedRecipeId.set(r.id);
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

  back() {
    this.router.navigate(['/calendar']);
  }
}
