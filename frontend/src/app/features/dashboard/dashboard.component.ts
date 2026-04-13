import { Component, inject } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary">
      <span>🍺 BrewMaster</span>
      <span class="spacer"></span>
      <button mat-icon-button (click)="auth.logout()" title="Sign out">
        <mat-icon>logout</mat-icon>
      </button>
    </mat-toolbar>

    <div class="dashboard-content">
      <h2>Welcome, {{ auth.currentUser()?.displayName ?? auth.currentUser()?.username }}!</h2>
      <p>Dashboard coming soon — Calendar, Recipes, Brew Mode, and Orders.</p>
    </div>
  `,
  styles: [`
    .dashboard-content {
      padding: 24px;
    }
    .spacer { flex: 1 1 auto; }
  `]
})
export class DashboardComponent {
  readonly auth = inject(AuthService);
}
