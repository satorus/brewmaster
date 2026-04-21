import { Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { DecimalPipe, DatePipe } from '@angular/common';
import { OrderService } from './order.service';
import { OrderResultDto, OrderSummaryDto, shopSearchUrl } from '../../core/models/order.model';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [
    MatToolbarModule, MatButtonModule, MatIconModule,
    MatCardModule, MatProgressSpinnerModule, MatDividerModule,
    MatChipsModule, DecimalPipe, DatePipe,
  ],
  template: `
    <mat-toolbar color="primary">
      <button mat-icon-button (click)="back()"><mat-icon>arrow_back</mat-icon></button>
      <span>Order History</span>
      <span class="spacer"></span>
      <button mat-icon-button (click)="newOrder()" title="New order">
        <mat-icon>add</mat-icon>
      </button>
    </mat-toolbar>

    @if (loading()) {
      <div class="center"><mat-spinner diameter="40"></mat-spinner></div>
    } @else if (selectedOrder()) {
      <div class="container">
        <button mat-button (click)="selectedOrder.set(null)" class="back-btn">
          <mat-icon>arrow_back</mat-icon> All Orders
        </button>

        <mat-card class="summary-card">
          <mat-card-content>
            <div class="summary-row">
              <span class="recipe-label">{{ selectedOrder()!.recipeName }}</span>
              <span class="volume-label">{{ selectedOrder()!.volumeL }}L</span>
            </div>
            <div class="total-row">
              <mat-icon>euro</mat-icon>
              <span class="total-label">
                {{ selectedOrder()!.estimatedTotalMin | number:'1.2-2' }} –
                {{ selectedOrder()!.estimatedTotalMax | number:'1.2-2' }} EUR
              </span>
            </div>
            <p class="meta">Generated {{ selectedOrder()!.generatedAt | date:'medium' }}</p>
            <p class="disclaimer">{{ selectedOrder()!.disclaimer }}</p>
          </mat-card-content>
        </mat-card>

        @for (item of selectedOrder()!.items; track item.ingredientName) {
          <mat-card class="item-card">
            <mat-card-content>
              <div class="item-header">
                <span class="ingredient-name">{{ item.ingredientName }}</span>
                <mat-chip class="amount-chip">{{ item.requiredAmount | number:'1.0-3' }} {{ item.unit }}</mat-chip>
              </div>
              @if (item.bestOffer) {
                <div class="offer-row">
                  <mat-icon class="star-icon">star</mat-icon>
                  <span class="shop-name">{{ item.bestOffer.shopName }}</span>
                  <span class="price">{{ item.bestOffer.totalCost | number:'1.2-2' }} EUR</span>
                </div>
                <a [href]="searchUrl(item.bestOffer.shopDomain, item.ingredientName)" target="_blank" rel="noopener" class="shop-link">
                  <mat-icon>search</mat-icon> Search on {{ item.bestOffer.shopName }}
                </a>
              } @else {
                <span class="no-offer">No price found</span>
              }
            </mat-card-content>
          </mat-card>
        }
      </div>
    } @else if (orders().length === 0) {
      <div class="empty-state">
        <mat-icon class="empty-icon">shopping_cart</mat-icon>
        <p>No orders yet</p>
        <button mat-raised-button color="primary" (click)="newOrder()">Create First Order</button>
      </div>
    } @else {
      <div class="container">
        @for (order of orders(); track order.id) {
          <mat-card class="history-card" (click)="openOrder(order.id)">
            <mat-card-content>
              <div class="history-row">
                <div class="history-info">
                  <span class="recipe-name">{{ order.recipeName }}</span>
                  <span class="meta">{{ order.volumeL }}L · {{ order.generatedAt | date:'mediumDate' }}</span>
                </div>
                <div class="history-total">
                  {{ order.estimatedTotalMin | number:'1.0-0' }}–{{ order.estimatedTotalMax | number:'1.0-0' }} EUR
                </div>
              </div>
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
    .center { display: flex; justify-content: center; align-items: center; flex: 1; }
    .back-btn { color: rgba(255,255,255,.7); margin-bottom: 12px; }
    .summary-card, .item-card, .history-card {
      background: #2a2a2a !important;
      color: rgba(255,255,255,.87) !important;
      margin-bottom: 12px;
    }
    .history-card { cursor: pointer; }
    .history-card:hover { background: #333 !important; }

    .summary-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .recipe-label, .recipe-name { font-size: 16px; font-weight: 500; color: rgba(255,255,255,.87); }
    .volume-label { font-size: 14px; color: rgba(255,255,255,.6); }
    .total-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
    .total-row mat-icon { color: #ffa726; }
    .total-label { font-size: 18px; font-weight: 600; color: rgba(255,255,255,.87); }
    .meta { font-size: 12px; color: rgba(255,255,255,.45); margin: 4px 0; }
    .disclaimer { font-size: 11px; color: rgba(255,255,255,.4); margin: 0; }

    .item-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .ingredient-name { font-size: 15px; font-weight: 500; }
    .amount-chip { background: rgba(255,255,255,.1) !important; color: rgba(255,255,255,.7) !important; font-size: 12px !important; }
    .offer-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
    .star-icon { font-size: 16px; width: 16px; height: 16px; color: #ffa726; }
    .shop-name { flex: 1; font-size: 13px; color: rgba(255,255,255,.7); }
    .price { font-size: 14px; font-weight: 600; }
    .shop-link { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #C17817; text-decoration: none; padding-left: 24px; }
    .shop-link mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .no-offer { font-size: 13px; color: rgba(255,255,255,.4); }

    .history-row { display: flex; justify-content: space-between; align-items: center; }
    .history-info { display: flex; flex-direction: column; gap: 4px; }
    .history-total { font-size: 14px; font-weight: 600; color: #ffa726; }

    .empty-state {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 16px;
      color: rgba(255,255,255,.45);
    }
    .empty-icon { font-size: 64px; width: 64px; height: 64px; opacity: .3; }
  `]
})
export class OrderHistoryComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly orderService = inject(OrderService);

  readonly loading = signal(true);
  readonly orders = signal<OrderSummaryDto[]>([]);
  readonly selectedOrder = signal<OrderResultDto | null>(null);

  ngOnInit() {
    this.orderService.list().subscribe({
      next: page => {
        this.orders.set(page.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openOrder(id: string) {
    this.orderService.getById(id).subscribe({
      next: order => this.selectedOrder.set(order),
      error: () => {}
    });
  }

  newOrder() {
    this.router.navigate(['/order/new']);
  }

  searchUrl(shopDomain: string, ingredient: string): string {
    return shopSearchUrl(shopDomain, ingredient);
  }

  back() {
    this.router.navigate(['/calendar']);
  }
}
