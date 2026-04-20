import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule],
  template: `
    <div class="content">
      <router-outlet />
    </div>
    @if (auth.isAuthenticated() && !isBrewSession()) {
      <nav class="bottom-nav" aria-label="Main navigation">
        <a routerLink="/calendar" routerLinkActive="active" class="nav-item">
          <mat-icon>calendar_month</mat-icon>
          <span>Calendar</span>
        </a>
        <a routerLink="/recipes" routerLinkActive="active" class="nav-item">
          <mat-icon>menu_book</mat-icon>
          <span>Recipes</span>
        </a>
        <a routerLink="/recipe-finder" routerLinkActive="active" class="nav-item">
          <mat-icon>auto_awesome</mat-icon>
          <span>Finder</span>
        </a>
        <a routerLink="/brew-mode/setup" routerLinkActive="active" class="nav-item">
          <mat-icon>science</mat-icon>
          <span>Brew</span>
        </a>
        <a routerLink="/order/new" class="nav-item" [class.active]="isOrderRoute()">
          <mat-icon>shopping_cart</mat-icon>
          <span>Order</span>
        </a>
      </nav>
    }
  `,
  styles: [`
    :host { display: flex; flex-direction: column; height: 100vh; }
    .content { flex: 1; overflow: auto; min-height: 0; }
    .bottom-nav {
      display: flex;
      height: 56px;
      background: #2C2C2C;
      border-top: 1px solid #333;
      flex-shrink: 0;
    }
    .nav-item {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 2px;
      text-decoration: none;
      color: rgba(255,255,255,.45);
      font-size: 10px;
      font-family: Roboto, sans-serif;
      letter-spacing: .03em;
      cursor: pointer;
      transition: color .15s;
      user-select: none;
    }
    .nav-item mat-icon { font-size: 22px; width: 22px; height: 22px; }
    .nav-item.active { color: #C17817; }
    .nav-item.disabled { opacity: .3; cursor: not-allowed; }
    .nav-item:not(.disabled):hover { color: rgba(193,120,23,.7); }
  `]
})
export class AppComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  isBrewSession(): boolean {
    return this.router.url.startsWith('/brew-mode/session');
  }

  isOrderRoute(): boolean {
    return this.router.url.startsWith('/order');
  }
}
