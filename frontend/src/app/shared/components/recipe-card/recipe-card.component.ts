import { Component, input, output } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { RecipeSummary } from '../../../core/models/recipe.model';

@Component({
  selector: 'app-recipe-card',
  standalone: true,
  imports: [MatCardModule, MatIconModule, MatChipsModule],
  template: `
    <mat-card class="recipe-card" (click)="clicked.emit(recipe())">
      <mat-card-content>
        <div class="card-row">
          <div class="card-info">
            <div class="recipe-name">{{ recipe().name }}</div>
            @if (recipe().style) {
              <div class="recipe-style">{{ recipe().style }}</div>
            }
            <div class="stats-row">
              @if (recipe().abv != null) {
                <span class="stat"><mat-icon>local_bar</mat-icon>{{ recipe().abv }}%</span>
              }
              @if (recipe().ibu != null) {
                <span class="stat"><mat-icon>science</mat-icon>{{ recipe().ibu }} IBU</span>
              }
              <span class="stat"><mat-icon>water_drop</mat-icon>{{ recipe().baseVolumeL }}L</span>
            </div>
          </div>
          @if (recipe().aiGenerated) {
            <mat-chip class="ai-chip">AI</mat-chip>
          }
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .recipe-card {
      background: #2C2C2C !important;
      cursor: pointer;
      transition: transform .15s, box-shadow .15s;
      margin-bottom: 10px;
    }
    .recipe-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,.5); }
    .card-row { display: flex; align-items: center; justify-content: space-between; }
    .card-info { flex: 1; min-width: 0; }
    .recipe-name { font-size: 16px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .recipe-style { font-size: 13px; color: rgba(255,255,255,.5); margin-top: 2px; }
    .stats-row { display: flex; gap: 12px; margin-top: 8px; flex-wrap: wrap; }
    .stat { display: flex; align-items: center; gap: 3px; font-size: 12px; color: rgba(255,255,255,.6); }
    .stat mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .ai-chip { background: #3E2723 !important; color: #C17817 !important; font-size: 11px; font-weight: 700; }
  `]
})
export class RecipeCardComponent {
  recipe = input.required<RecipeSummary>();
  clicked = output<RecipeSummary>();
}
