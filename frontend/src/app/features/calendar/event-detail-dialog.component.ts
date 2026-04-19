import { Component, computed, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { DatePipe } from '@angular/common';
import { BrewEventResponse } from '../../core/models/event.model';
import { EventService } from './event.service';
import { Router } from '@angular/router';

export interface EventDetailData {
  event: BrewEventResponse;
  currentUserId: string;
}

@Component({
  selector: 'app-event-detail-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    DatePipe,
  ],
  template: `
    <h2 mat-dialog-title>{{ event().title }}</h2>

    <mat-dialog-content>
      <div class="meta">
        <div class="meta-row">
          <mat-icon>event</mat-icon>
          <span>{{ event().brewDate | date:'fullDate' }}</span>
        </div>
        @if (event().startTime) {
          <div class="meta-row">
            <mat-icon>access_time</mat-icon>
            <span>{{ event().startTime!.substring(0, 5) }}</span>
          </div>
        }
        @if (event().location) {
          <div class="meta-row">
            <mat-icon>place</mat-icon>
            <span>{{ event().location }}</span>
          </div>
        }
        @if (event().description) {
          <p class="description">{{ event().description }}</p>
        }
      </div>

      <mat-divider></mat-divider>

      <h3 class="section-title">Participants ({{ event().participants.length }})</h3>
      <mat-list>
        @for (p of event().participants; track p.userId) {
          <mat-list-item>
            <span matListItemTitle>{{ p.displayName ?? p.username }}</span>
            <span matListItemLine class="rsvp-line">
              <span [class]="'rsvp-badge rsvp-' + p.rsvp.toLowerCase()">{{ p.rsvp }}</span>
            </span>
          </mat-list-item>
        }
        @if (event().participants.length === 0) {
          <mat-list-item><span matListItemTitle>No participants yet</span></mat-list-item>
        }
      </mat-list>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      @if (event().recipeId) {
        <button mat-button color="accent" (click)="startBrew()">
          <mat-icon>science</mat-icon> Start Brew
        </button>
      }
      @if (isCreator()) {
        <button mat-button color="warn" (click)="onDelete()" [disabled]="isSubmitting()">
          Delete
        </button>
        <button mat-button (click)="onEdit()">Edit</button>
      } @else {
        @if (myRsvp() !== 'ACCEPTED') {
          <button mat-button color="primary" (click)="onRsvp('ACCEPTED')" [disabled]="isSubmitting()">
            @if (isSubmitting()) { <mat-spinner diameter="16" style="display:inline-block"></mat-spinner> }
            @else { Accept }
          </button>
        }
        @if (myRsvp() !== 'DECLINED') {
          <button mat-button color="warn" (click)="onRsvp('DECLINED')" [disabled]="isSubmitting()">
            @if (isSubmitting()) { <mat-spinner diameter="16" style="display:inline-block"></mat-spinner> }
            @else { Decline }
          </button>
        }
      }
      <button mat-button mat-dialog-close>Close</button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content { min-width: min(480px, 90vw); }
    .meta { margin-bottom: 16px; }
    .meta-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; color: rgba(255,255,255,.7); }
    .meta-row mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .description { margin-top: 12px; color: rgba(255,255,255,.7); }
    .section-title { margin: 16px 0 4px; font-size: 14px; font-weight: 500; color: rgba(255,255,255,.6); text-transform: uppercase; letter-spacing: .08em; }
    .rsvp-line { margin-top: 2px; }
    .rsvp-badge { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 10px; text-transform: uppercase; letter-spacing: .04em; }
    .rsvp-accepted { background: #4CAF50; color: #fff; }
    .rsvp-pending  { background: #FFC107; color: #000; }
    .rsvp-declined { background: #555; color: #ccc; }
  `]
})
export class EventDetailDialogComponent {
  readonly dialogRef = inject(MatDialogRef<EventDetailDialogComponent>);
  private readonly data: EventDetailData = inject(MAT_DIALOG_DATA);
  private readonly eventService = inject(EventService);
  private readonly router = inject(Router);

  event = signal<BrewEventResponse>(this.data.event);
  isSubmitting = signal(false);

  isCreator = computed(() => this.event().createdBy === this.data.currentUserId);
  myRsvp = computed(() => {
    const p = this.event().participants.find(p => p.userId === this.data.currentUserId);
    return p?.rsvp ?? 'NONE';
  });

  onRsvp(status: 'ACCEPTED' | 'DECLINED'): void {
    this.isSubmitting.set(true);
    this.eventService.rsvp(this.event().id, status).subscribe({
      next: updated => {
        this.event.set(updated);
        this.isSubmitting.set(false);
      },
      error: () => this.isSubmitting.set(false),
    });
  }

  startBrew(): void {
    this.dialogRef.close();
    this.router.navigate(['/brew-mode/setup'], {
      queryParams: { recipeId: this.event().recipeId },
    });
  }

  onEdit(): void {
    this.dialogRef.close({ action: 'edit', event: this.event() });
  }

  onDelete(): void {
    this.dialogRef.close({ action: 'delete', eventId: this.event().id });
  }
}
