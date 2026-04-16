import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BrewEventResponse, CreateEventRequest, UpdateEventRequest } from '../../core/models/event.model';
import { EventService } from './event.service';

@Component({
  selector: 'app-create-event-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatDatepickerModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data?.id ? 'Edit Event' : 'New Brew Event' }}</h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="event-form">

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Title</mat-label>
          <input matInput formControlName="title" placeholder="IPA Brew Day">
          @if (form.get('title')?.invalid && form.get('title')?.touched) {
            <mat-error>Title is required (max 200 chars)</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Date</mat-label>
          <input matInput [matDatepicker]="picker" formControlName="brewDate">
          <mat-datepicker-toggle matIconSuffix [for]="picker"></mat-datepicker-toggle>
          <mat-datepicker #picker></mat-datepicker>
          @if (form.get('brewDate')?.invalid && form.get('brewDate')?.touched) {
            <mat-error>Date is required</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Start Time</mat-label>
          <input matInput type="time" formControlName="startTime">
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Location</mat-label>
          <input matInput formControlName="location" placeholder="My Garage">
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="3" placeholder="Optional notes..."></textarea>
        </mat-form-field>

      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary"
              (click)="submit()"
              [disabled]="form.invalid || isSubmitting()">
        @if (isSubmitting()) {
          <mat-spinner diameter="18" style="display:inline-block"></mat-spinner>
        } @else {
          {{ data?.id ? 'Save Changes' : 'Create Event' }}
        }
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .event-form { display: flex; flex-direction: column; gap: 4px; padding-top: 8px; }
    .full-width { width: 100%; }
    mat-dialog-content { min-width: min(480px, 90vw); }
  `]
})
export class CreateEventDialogComponent {
  readonly dialogRef = inject(MatDialogRef<CreateEventDialogComponent>);
  readonly data: BrewEventResponse | null = inject(MAT_DIALOG_DATA);
  private readonly eventService = inject(EventService);

  isSubmitting = signal(false);

  form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    brewDate: new FormControl<Date | null>(null, { validators: [Validators.required] }),
    startTime: new FormControl('', { nonNullable: true }),
    location: new FormControl('', { nonNullable: true }),
    description: new FormControl('', { nonNullable: true }),
  });

  constructor() {
    if (this.data) {
      this.form.patchValue({
        title: this.data.title,
        brewDate: new Date(this.data.brewDate + 'T12:00:00'),
        startTime: this.data.startTime?.substring(0, 5) ?? '',
        location: this.data.location ?? '',
        description: this.data.description ?? '',
      });
    }
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const v = this.form.getRawValue();
    const brewDate = v.brewDate instanceof Date
      ? v.brewDate.toLocaleDateString('en-CA')  // yields YYYY-MM-DD
      : '';

    this.isSubmitting.set(true);

    if (this.data?.id) {
      const req: UpdateEventRequest = {
        title: v.title,
        brewDate,
        description: v.description || undefined,
        startTime: v.startTime || undefined,
        location: v.location || undefined,
      };
      this.eventService.updateEvent(this.data.id, req).subscribe({
        next: event => { this.isSubmitting.set(false); this.dialogRef.close(event); },
        error: () => this.isSubmitting.set(false),
      });
    } else {
      const req: CreateEventRequest = {
        title: v.title,
        brewDate,
        description: v.description || undefined,
        startTime: v.startTime || undefined,
        location: v.location || undefined,
      };
      this.eventService.createEvent(req).subscribe({
        next: event => { this.isSubmitting.set(false); this.dialogRef.close(event); },
        error: () => this.isSubmitting.set(false),
      });
    }
  }
}
