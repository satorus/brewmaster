import { Component, input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    @if (loading()) {
      <div class="spinner-overlay">
        <mat-spinner [diameter]="diameter()" />
      </div>
    }
  `,
  styles: [`
    .spinner-overlay {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 32px;
    }
  `]
})
export class LoadingSpinnerComponent {
  readonly loading = input<boolean>(false);
  readonly diameter = input<number>(48);
}
