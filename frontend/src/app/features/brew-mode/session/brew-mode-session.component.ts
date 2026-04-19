import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe, DecimalPipe } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BrewSessionService } from '../brew-session.service';
import { BrewTimerService } from '../brew-timer.service';
import { BrewSessionResponse, PHASE_COLORS, Phase, StepLogEntry } from '../../../core/models/brew-session.model';
import { StepDto } from '../../../core/models/recipe.model';
import { IngredientPlaceholderPipe } from '../../../shared/pipes/ingredient-placeholder.pipe';
import { HasCanDeactivate } from '../brew-session.guard';

@Component({
  selector: 'app-brew-mode-session',
  standalone: true,
  imports: [
    FormsModule, DatePipe, DecimalPipe,
    MatToolbarModule, MatButtonModule, MatIconModule,
    MatCardModule, MatProgressSpinnerModule, MatChipsModule,
    MatFormFieldModule, MatInputModule, MatSnackBarModule,
    IngredientPlaceholderPipe,
  ],
  template: `
    <div class="session-root">
      <mat-toolbar [style.background]="currentPhaseColor()" class="session-toolbar">
        <button mat-icon-button (click)="navigateAway()"><mat-icon>close</mat-icon></button>
        <span class="toolbar-title">{{ session()?.status === 'IN_PROGRESS' ? 'Brew Mode' : 'Session Complete' }}</span>
        <span class="spacer"></span>
        @if (session()?.status === 'IN_PROGRESS') {
          <span class="step-counter">{{ viewStep() + 1 }}/{{ totalSteps() }}</span>
        }
      </mat-toolbar>

      @if (loading()) {
        <div class="center"><mat-spinner></mat-spinner></div>
      } @else if (error()) {
        <div class="center error-state">
          <mat-icon>error_outline</mat-icon>
          <p>{{ error() }}</p>
        </div>
      } @else if (session()?.status !== 'IN_PROGRESS' && session()?.status !== undefined) {
        <!-- Completion / Abandoned screen -->
        <div class="complete-screen">
          <mat-icon class="complete-icon">{{ session()!.status === 'COMPLETED' ? 'check_circle' : 'cancel' }}</mat-icon>
          <h2>{{ session()!.status === 'COMPLETED' ? 'Brew Complete!' : 'Session Abandoned' }}</h2>
          <p class="complete-sub">{{ session()!.scaledSteps.length }} steps &bull; {{ elapsedTime() }}</p>
          <div class="log-list">
            @for (log of session()!.stepLogs; track log.stepNumber) {
              <div class="log-entry">
                <span>Step {{ log.stepNumber + 1 }}</span>
                <span>{{ log.completedAt | date:'HH:mm' }}</span>
              </div>
            }
          </div>
          <button mat-raised-button color="primary" (click)="goHome()">Back to Recipes</button>
        </div>
      } @else if (currentStep()) {
        <!-- Phase transition overlay -->
        @if (showPhaseTransition()) {
          <div class="phase-overlay" [style.background]="currentPhaseColor()">
            <div class="phase-name">{{ currentStep()!.phase }}</div>
            <div class="phase-sub">Get ready for the next phase</div>
          </div>
        }

        <!-- Step card -->
        <div class="step-scroll">
          <div class="phase-badge" [style.background]="currentPhaseColor()">
            {{ currentStep()!.phase }}
          </div>

          <div class="step-content">
            <div class="step-number">Step {{ viewStep() + 1 }} of {{ totalSteps() }}</div>
            <h2 class="step-title">{{ currentStep()!.title }}</h2>

            <p class="step-instructions">
              {{ currentStep()!.instructions | ingredientPlaceholder : (session()?.scaledIngredients ?? []) }}
            </p>

            @if (currentStep()!.targetTempC) {
              <mat-chip class="temp-chip">
                <mat-icon>thermostat</mat-icon>
                {{ currentStep()!.targetTempC }}°C
              </mat-chip>
            }

            @if (currentStep()!.timerRequired && currentStep()!.durationMin) {
              <div class="timer-section">
                <div class="timer-display" [class.running]="timer.running()" (click)="toggleTimer()">
                  {{ timer.formatTime(timer.running() || timer.remainingSeconds() > 0
                      ? timer.remainingSeconds()
                      : currentStep()!.durationMin! * 60) }}
                </div>
                <div class="timer-hint">
                  {{ timer.running() ? 'Tap to pause' : (timer.remainingSeconds() > 0 ? 'Tap to resume' : 'Tap to start') }}
                </div>
              </div>
            }

            <mat-form-field appearance="outline" class="notes-field">
              <mat-label>Step notes (optional)</mat-label>
              <textarea matInput [(ngModel)]="stepNotes" rows="2"></textarea>
            </mat-form-field>

            @if (currentStep()!.targetTempC) {
              <mat-form-field appearance="outline" class="temp-field">
                <mat-label>Actual temp (°C)</mat-label>
                <input matInput type="number" [(ngModel)]="actualTemp">
              </mat-form-field>
            }
          </div>

          <div class="step-actions">
            @if (viewStep() > 0) {
              <button mat-stroked-button (click)="prevStep()">
                <mat-icon>chevron_left</mat-icon> Previous
              </button>
            }
            <span class="spacer"></span>
            @if (viewStep() < totalSteps() - 1) {
              <button mat-raised-button color="primary" (click)="completeStep()" [disabled]="advancing()">
                @if (advancing()) { <mat-spinner diameter="20"></mat-spinner> }
                @else { Complete Step <mat-icon>chevron_right</mat-icon> }
              </button>
            } @else {
              <button mat-raised-button color="accent" (click)="finishSession()" [disabled]="advancing()">
                @if (advancing()) { <mat-spinner diameter="20"></mat-spinner> }
                @else { Finish Brew <mat-icon>check</mat-icon> }
              </button>
            }
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .session-root { display: flex; flex-direction: column; height: 100vh; background: #1a1a1a; }
    .session-toolbar { transition: background .4s; }
    .toolbar-title { flex: 1; margin-left: 8px; }
    .spacer { flex: 1; }
    .step-counter { font-size: 13px; opacity: .8; }
    .center { display: flex; justify-content: center; align-items: center; flex: 1; }
    .error-state { flex-direction: column; color: #f44336; gap: 8px; }

    .step-scroll { flex: 1; overflow-y: auto; display: flex; flex-direction: column; }
    .phase-badge {
      padding: 6px 16px; font-size: 11px; font-weight: 700; letter-spacing: 1.5px;
      text-transform: uppercase; color: white; text-align: center;
    }
    .step-content { padding: 20px 16px; flex: 1; }
    .step-number { font-size: 12px; color: rgba(255,255,255,.5); margin-bottom: 8px; }
    .step-title { font-size: 22px; font-weight: 600; color: white; margin: 0 0 16px; }
    .step-instructions { font-size: 15px; color: rgba(255,255,255,.85); line-height: 1.6; margin-bottom: 16px; white-space: pre-wrap; }
    .temp-chip { background: rgba(255,255,255,.1); color: white; margin-bottom: 16px; }
    .temp-chip mat-icon { font-size: 16px; width: 16px; height: 16px; margin-right: 4px; }

    .timer-section { text-align: center; margin: 16px 0; }
    .timer-display {
      font-size: 56px; font-weight: 300; color: white; cursor: pointer;
      font-variant-numeric: tabular-nums; padding: 8px;
      border-radius: 8px; transition: background .2s;
    }
    .timer-display:hover { background: rgba(255,255,255,.08); }
    .timer-display.running { color: #81c784; }
    .timer-hint { font-size: 12px; color: rgba(255,255,255,.4); }

    .notes-field, .temp-field { width: 100%; margin-top: 8px; }
    .notes-field textarea, .temp-field input { color: white; }

    .step-actions {
      display: flex; align-items: center; padding: 12px 16px;
      border-top: 1px solid rgba(255,255,255,.1); gap: 8px;
    }
    .step-actions button { height: 44px; }

    .phase-overlay {
      position: fixed; inset: 0; z-index: 100;
      display: flex; flex-direction: column;
      align-items: center; justify-content: center;
      animation: fadeInOut 2s forwards;
    }
    .phase-name { font-size: 36px; font-weight: 700; color: white; letter-spacing: 2px; }
    .phase-sub { font-size: 14px; color: rgba(255,255,255,.7); margin-top: 8px; }
    @keyframes fadeInOut {
      0% { opacity: 0; } 20% { opacity: 1; } 80% { opacity: 1; } 100% { opacity: 0; pointer-events: none; }
    }

    .complete-screen {
      flex: 1; display: flex; flex-direction: column;
      align-items: center; justify-content: center; padding: 32px;
      gap: 16px; text-align: center;
    }
    .complete-icon { font-size: 64px; width: 64px; height: 64px; color: #81c784; }
    .complete-screen h2 { font-size: 28px; color: white; margin: 0; }
    .complete-sub { color: rgba(255,255,255,.6); }
    .log-list { width: 100%; max-width: 360px; }
    .log-entry {
      display: flex; justify-content: space-between;
      padding: 6px 0; border-bottom: 1px solid rgba(255,255,255,.1);
      color: rgba(255,255,255,.7); font-size: 13px;
    }
  `]
})
export class BrewModeSessionComponent implements OnInit, OnDestroy, HasCanDeactivate {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private sessionService = inject(BrewSessionService);
  private snackBar = inject(MatSnackBar);
  readonly timer = inject(BrewTimerService);

  readonly loading = signal(true);
  readonly advancing = signal(false);
  readonly error = signal<string | null>(null);
  readonly session = signal<BrewSessionResponse | null>(null);
  readonly viewStep = signal(0);
  readonly showPhaseTransition = signal(false);

  stepNotes = '';
  actualTemp: number | null = null;

  readonly totalSteps = computed(() => this.session()?.scaledSteps.length ?? 0);
  readonly currentStep = computed((): StepDto | null =>
    this.session()?.scaledSteps[this.viewStep()] ?? null);
  readonly currentPhaseColor = computed(() => {
    const phase = this.currentStep()?.phase as Phase;
    return phase ? PHASE_COLORS[phase] ?? '#333' : '#333';
  });
  readonly elapsedTime = computed(() => {
    const s = this.session();
    if (!s?.completedAt) return '';
    const ms = new Date(s.completedAt).getTime() - new Date(s.startedAt).getTime();
    const h = Math.floor(ms / 3600000);
    const m = Math.floor((ms % 3600000) / 60000);
    return h > 0 ? `${h}h ${m}m` : `${m}m`;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.sessionService.getSession(id).subscribe({
      next: s => {
        this.session.set(s);
        this.viewStep.set(Math.min(s.currentStep, Math.max(0, (s.scaledSteps.length ?? 1) - 1)));
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Session not found.');
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.timer.stop();
  }

  canDeactivate(): boolean {
    const s = this.session();
    return !s || s.status !== 'IN_PROGRESS';
  }

  completeStep(): void {
    const s = this.session();
    if (!s) return;
    this.advancing.set(true);
    const prevPhase = this.currentStep()?.phase;
    this.sessionService.advanceStep(s.id, {
      stepNumber: this.viewStep(),
      actualTempC: this.actualTemp ?? undefined,
      notes: this.stepNotes || undefined,
    }).subscribe({
      next: updated => {
        this.session.set(updated);
        const nextStep = this.viewStep() + 1;
        if (nextStep < updated.scaledSteps.length) {
          const nextPhase = updated.scaledSteps[nextStep]?.phase;
          if (nextPhase && nextPhase !== prevPhase) {
            this.showPhaseTransition.set(true);
            setTimeout(() => this.showPhaseTransition.set(false), 2000);
          }
          this.viewStep.set(nextStep);
        } else {
          this.viewStep.set(updated.scaledSteps.length - 1);
        }
        this.stepNotes = '';
        this.actualTemp = null;
        this.timer.reset();
        this.advancing.set(false);
      },
      error: err => {
        this.advancing.set(false);
        this.snackBar.open(err.error?.message ?? 'Failed to advance step', 'Dismiss', { duration: 3000 });
      },
    });
  }

  finishSession(): void {
    const s = this.session();
    if (!s) return;
    this.advancing.set(true);
    this.sessionService.completeSession(s.id, this.stepNotes || undefined).subscribe({
      next: updated => {
        this.session.set(updated);
        this.timer.stop();
        this.advancing.set(false);
      },
      error: err => {
        this.advancing.set(false);
        this.snackBar.open(err.error?.message ?? 'Failed to complete session', 'Dismiss', { duration: 3000 });
      },
    });
  }

  prevStep(): void {
    if (this.viewStep() > 0) {
      this.viewStep.update(v => v - 1);
      this.timer.reset();
    }
  }

  toggleTimer(): void {
    const step = this.currentStep();
    if (!step?.durationMin) return;
    if (this.timer.running()) {
      this.timer.stop();
    } else if (this.timer.remainingSeconds() > 0) {
      this.timer.start(this.timer.remainingSeconds());
    } else {
      this.timer.start(step.durationMin * 60);
    }
  }

  navigateAway(): void {
    if (this.canDeactivate() || confirm('Leave brew session? Your progress is saved and you can return.')) {
      this.router.navigate(['/recipes']);
    }
  }

  goHome(): void {
    this.router.navigate(['/recipes']);
  }
}
