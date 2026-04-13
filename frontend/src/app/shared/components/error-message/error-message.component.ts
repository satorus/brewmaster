import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-error-message',
  standalone: true,
  imports: [MatIconModule],
  template: `
    @if (message()) {
      <div class="error-banner">
        <mat-icon>error_outline</mat-icon>
        <span>{{ message() }}</span>
      </div>
    }
  `,
  styles: [`
    .error-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 14px;
      background: rgba(244, 67, 54, 0.12);
      border: 1px solid rgba(244, 67, 54, 0.4);
      border-radius: 4px;
      color: #f44336;
      font-size: 0.875rem;
      margin-bottom: 8px;

      mat-icon {
        font-size: 18px;
        height: 18px;
        width: 18px;
        flex-shrink: 0;
      }
    }
  `]
})
export class ErrorMessageComponent {
  readonly message = input<string | null>(null);
}
