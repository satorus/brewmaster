import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class BrewTimerService {
  readonly remainingSeconds = signal(0);
  readonly running = signal(false);

  private intervalId: ReturnType<typeof setInterval> | null = null;

  start(durationSeconds: number): void {
    this.stop();
    this.remainingSeconds.set(durationSeconds);
    this.running.set(true);
    this.intervalId = setInterval(() => {
      const remaining = this.remainingSeconds() - 1;
      if (remaining <= 0) {
        this.remainingSeconds.set(0);
        this.running.set(false);
        clearInterval(this.intervalId!);
        this.intervalId = null;
        this.beep();
      } else {
        this.remainingSeconds.set(remaining);
      }
    }, 1000);
  }

  stop(): void {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
    this.running.set(false);
  }

  reset(): void {
    this.stop();
    this.remainingSeconds.set(0);
  }

  formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60).toString().padStart(2, '0');
    const s = (seconds % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  private beep(): void {
    try {
      const ctx = new AudioContext();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.frequency.value = 880;
      osc.type = 'sine';
      gain.gain.setValueAtTime(0.3, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.8);
      osc.start(ctx.currentTime);
      osc.stop(ctx.currentTime + 0.8);
    } catch {
      // Audio not available
    }
  }
}
