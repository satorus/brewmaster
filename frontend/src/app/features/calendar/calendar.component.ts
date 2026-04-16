import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AuthService } from '../../core/auth/auth.service';
import { EventService } from './event.service';
import { BrewEventResponse } from '../../core/models/event.model';
import { CreateEventDialogComponent } from './create-event-dialog.component';
import { EventDetailDialogComponent } from './event-detail-dialog.component';

interface CalendarDay {
  date: Date;
  dateStr: string;
  isCurrentMonth: boolean;
  isToday: boolean;
  events: BrewEventResponse[];
}

@Component({
  selector: 'app-calendar',
  standalone: true,
  imports: [
    DatePipe,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatSnackBarModule,
    MatDialogModule,
  ],
  template: `
    <!-- Toolbar -->
    <mat-toolbar color="primary" class="toolbar">
      <span class="app-title">🍺 BrewMaster</span>
      <span class="spacer"></span>
      <div class="month-nav">
        <button mat-icon-button (click)="prevMonth()" aria-label="Previous month">
          <mat-icon>chevron_left</mat-icon>
        </button>
        <span class="month-label">{{ formatMonth() }}</span>
        <button mat-icon-button (click)="nextMonth()" aria-label="Next month">
          <mat-icon>chevron_right</mat-icon>
        </button>
      </div>
      <button mat-icon-button
              (click)="toggleView()"
              [matTooltip]="viewMode() === 'month' ? 'Switch to list view' : 'Switch to calendar view'">
        <mat-icon>{{ viewMode() === 'month' ? 'view_list' : 'calendar_month' }}</mat-icon>
      </button>
      <button mat-icon-button (click)="authService.logout()" matTooltip="Sign out">
        <mat-icon>logout</mat-icon>
      </button>
    </mat-toolbar>

    <!-- Loading -->
    @if (isLoading()) {
      <div class="loading-container">
        <mat-spinner></mat-spinner>
      </div>
    } @else {

      <!-- Month view -->
      @if (viewMode() === 'month') {
        <div class="calendar-container">
          <div class="calendar-grid">
            @for (h of weekHeaders; track h) {
              <div class="day-header">{{ h }}</div>
            }
            @for (day of calendarDays(); track day.dateStr) {
              <div class="day-cell"
                   [class.other-month]="!day.isCurrentMonth"
                   [class.today]="day.isToday">
                <div class="day-number">{{ day.date.getDate() }}</div>
                @for (event of day.events; track event.id) {
                  <div [class]="'event-chip rsvp-' + getUserRsvp(event)"
                       (click)="openEventDetail(event)"
                       [matTooltip]="event.title">
                    {{ event.title }}
                  </div>
                }
              </div>
            }
          </div>
        </div>

      <!-- List view -->
      } @else {
        <div class="list-container">
          @if (events().length === 0) {
            <div class="empty-state">
              <mat-icon>event_busy</mat-icon>
              <p>No events this month</p>
              <button mat-flat-button color="primary" (click)="openCreateEvent()">
                Create First Event
              </button>
            </div>
          }
          @for (event of events(); track event.id) {
            <mat-card class="event-card" (click)="openEventDetail(event)">
              <mat-card-content>
                <div class="event-row">
                  <div class="date-block">
                    <span class="day-num">{{ event.brewDate | date:'d' }}</span>
                    <span class="month-abbr">{{ event.brewDate | date:'MMM' : 'UTC' }}</span>
                  </div>
                  <div class="event-info">
                    <div class="event-title">{{ event.title }}</div>
                    @if (event.startTime) {
                      <div class="event-meta-line">
                        <mat-icon>access_time</mat-icon>
                        {{ event.startTime.substring(0, 5) }}
                      </div>
                    }
                    @if (event.location) {
                      <div class="event-meta-line">
                        <mat-icon>place</mat-icon>
                        {{ event.location }}
                      </div>
                    }
                  </div>
                  <div [class]="'rsvp-pill rsvp-' + getUserRsvp(event)">
                    {{ getUserRsvp(event) }}
                  </div>
                </div>
              </mat-card-content>
            </mat-card>
          }
        </div>
      }
    }

    <!-- FAB -->
    <button mat-fab class="fab" color="primary" (click)="openCreateEvent()" aria-label="Create event">
      <mat-icon>add</mat-icon>
    </button>
  `,
  styles: [`
    :host { display: block; height: 100vh; background: #1A1A1A; overflow: auto; }

    .toolbar { position: sticky; top: 0; z-index: 10; }
    .app-title { font-size: 18px; font-weight: 600; }
    .spacer { flex: 1 1 auto; }
    .month-nav { display: flex; align-items: center; gap: 4px; }
    .month-label { min-width: 140px; text-align: center; font-size: 15px; font-weight: 500; }

    .loading-container { display: flex; justify-content: center; align-items: center; height: 60vh; }

    /* ── Month grid ── */
    .calendar-container { padding: 8px; }
    .calendar-grid {
      display: grid;
      grid-template-columns: repeat(7, 1fr);
      gap: 1px;
      background: #333;
      border: 1px solid #333;
      border-radius: 4px;
      overflow: hidden;
    }
    .day-header {
      background: #2C2C2C;
      padding: 8px 4px;
      text-align: center;
      font-size: 11px;
      font-weight: 600;
      color: rgba(255,255,255,.5);
      text-transform: uppercase;
      letter-spacing: .06em;
    }
    .day-cell {
      background: #1A1A1A;
      min-height: 80px;
      padding: 4px;
      cursor: default;
    }
    .day-cell.other-month { opacity: .35; }
    .day-cell.today .day-number {
      background: #C17817;
      border-radius: 50%;
      width: 22px;
      height: 22px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }
    .day-number { font-size: 12px; color: rgba(255,255,255,.7); margin-bottom: 3px; }
    .event-chip {
      font-size: 11px;
      padding: 2px 5px;
      border-radius: 3px;
      cursor: pointer;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-bottom: 2px;
      transition: opacity .15s;
    }
    .event-chip:hover { opacity: .8; }

    /* ── List view ── */
    .list-container { padding: 12px 16px; display: flex; flex-direction: column; gap: 10px; }
    .event-card { background: #2C2C2C !important; cursor: pointer; transition: transform .15s, box-shadow .15s; }
    .event-card:hover { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(0,0,0,.4); }
    .event-row { display: flex; align-items: center; gap: 16px; }
    .date-block { display: flex; flex-direction: column; align-items: center; min-width: 40px; }
    .day-num { font-size: 24px; font-weight: 700; line-height: 1; color: #C17817; }
    .month-abbr { font-size: 11px; color: rgba(255,255,255,.5); text-transform: uppercase; }
    .event-info { flex: 1; min-width: 0; }
    .event-title { font-size: 15px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .event-meta-line { display: flex; align-items: center; gap: 4px; font-size: 12px; color: rgba(255,255,255,.5); margin-top: 2px; }
    .event-meta-line mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .rsvp-pill {
      font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 12px;
      text-transform: uppercase; letter-spacing: .04em; white-space: nowrap;
    }

    /* ── RSVP colours (shared) ── */
    .rsvp-accepted { background: #4CAF50; color: #fff; }
    .rsvp-pending  { background: #FFC107; color: #000; }
    .rsvp-declined { background: #555; color: #ccc; }
    .rsvp-none     { background: #3E2723; color: #C17817; border: 1px solid #C17817; }

    /* ── Empty state ── */
    .empty-state {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; gap: 12px; height: 50vh; color: rgba(255,255,255,.4);
    }
    .empty-state mat-icon { font-size: 64px; width: 64px; height: 64px; }

    /* ── FAB ── */
    .fab { position: fixed; bottom: 24px; right: 24px; z-index: 20; }

    @media (max-width: 600px) {
      .month-label { min-width: 110px; font-size: 13px; }
      .day-cell { min-height: 56px; }
      .event-chip { font-size: 10px; padding: 1px 3px; }
    }
  `]
})
export class CalendarComponent implements OnInit {
  readonly authService = inject(AuthService);
  private readonly eventService = inject(EventService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly weekHeaders = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  currentMonth = signal(new Date());
  events = signal<BrewEventResponse[]>([]);
  isLoading = signal(false);
  viewMode = signal<'month' | 'list'>('month');

  calendarDays = computed<CalendarDay[]>(() => this.buildGrid(this.currentMonth(), this.events()));

  ngOnInit(): void {
    this.loadEvents();
  }

  formatMonth(): string {
    const d = this.currentMonth();
    return d.toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  prevMonth(): void {
    const d = new Date(this.currentMonth());
    d.setMonth(d.getMonth() - 1);
    this.currentMonth.set(d);
    this.loadEvents();
  }

  nextMonth(): void {
    const d = new Date(this.currentMonth());
    d.setMonth(d.getMonth() + 1);
    this.currentMonth.set(d);
    this.loadEvents();
  }

  toggleView(): void {
    this.viewMode.set(this.viewMode() === 'month' ? 'list' : 'month');
  }

  getUserRsvp(event: BrewEventResponse): string {
    const userId = this.authService.currentUser()?.id;
    if (!userId) return 'none';
    const p = event.participants.find(p => p.userId === userId);
    return (p?.rsvp ?? 'none').toLowerCase();
  }

  openEventDetail(event: BrewEventResponse): void {
    const ref = this.dialog.open(EventDetailDialogComponent, {
      data: { event, currentUserId: this.authService.currentUser()?.id ?? '' },
      width: '520px',
      maxWidth: '95vw',
      panelClass: 'dark-dialog',
    });

    ref.afterClosed().subscribe(result => {
      if (!result) return;
      if (result.action === 'edit') {
        this.openEditEvent(result.event);
      } else if (result.action === 'delete') {
        this.deleteEvent(result.eventId);
      } else {
        this.loadEvents();
      }
    });
  }

  openCreateEvent(): void {
    const ref = this.dialog.open(CreateEventDialogComponent, {
      data: null,
      width: '520px',
      maxWidth: '95vw',
      panelClass: 'dark-dialog',
    });
    ref.afterClosed().subscribe(created => { if (created) this.loadEvents(); });
  }

  private openEditEvent(event: BrewEventResponse): void {
    const ref = this.dialog.open(CreateEventDialogComponent, {
      data: event,
      width: '520px',
      maxWidth: '95vw',
      panelClass: 'dark-dialog',
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.loadEvents(); });
  }

  private deleteEvent(eventId: string): void {
    this.eventService.deleteEvent(eventId).subscribe({
      next: () => {
        this.loadEvents();
        this.snackBar.open('Event deleted', 'Close', { duration: 3000 });
      },
      error: () => this.snackBar.open('Failed to delete event', 'Close', { duration: 3000 }),
    });
  }

  private loadEvents(): void {
    const d = this.currentMonth();
    const month = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    this.isLoading.set(true);
    this.eventService.getEvents(month).subscribe({
      next: evts => { this.events.set(evts); this.isLoading.set(false); },
      error: () => { this.isLoading.set(false); this.snackBar.open('Failed to load events', 'Close', { duration: 3000 }); },
    });
  }

  private buildGrid(month: Date, events: BrewEventResponse[]): CalendarDay[] {
    const year = month.getFullYear();
    const m = month.getMonth();
    const firstDay = new Date(year, m, 1);

    const todayStr = new Date().toLocaleDateString('en-CA');

    // Offset to Monday (0 = Mon … 6 = Sun)
    const offset = (firstDay.getDay() + 6) % 7;
    const start = new Date(firstDay);
    start.setDate(start.getDate() - offset);

    const days: CalendarDay[] = [];
    const cursor = new Date(start);

    // 6 rows × 7 = 42 cells
    for (let i = 0; i < 42; i++) {
      const dateStr = cursor.toLocaleDateString('en-CA');
      days.push({
        date: new Date(cursor),
        dateStr,
        isCurrentMonth: cursor.getMonth() === m,
        isToday: dateStr === todayStr,
        events: events.filter(e => e.brewDate === dateStr),
      });
      cursor.setDate(cursor.getDate() + 1);
    }

    return days;
  }
}
