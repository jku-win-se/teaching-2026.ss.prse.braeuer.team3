import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Schedule } from '../../core/models';

@Component({
  selector: 'app-schedule-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatCheckboxModule, MatIconModule,
    FormsModule, ReactiveFormsModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.schedule ? 'Edit Schedule' : 'Add Schedule' }}</h2>
    <mat-dialog-content style="min-width:400px;padding-top:8px;">
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;">
        <mat-form-field appearance="outline">
          <mat-label>Schedule name</mat-label>
          <input matInput formControlName="name" placeholder="e.g. Morning Lights">
          <mat-hint>A short descriptive name</mat-hint>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Device</mat-label>
          <mat-select formControlName="deviceName">
            <mat-option value="Ceiling Light">Ceiling Light (Living Room)</mat-option>
            <mat-option value="TV">TV (Living Room)</mat-option>
            <mat-option value="Counter Light">Counter Light (Kitchen)</mat-option>
            <mat-option value="AC Unit">AC Unit (Bedroom)</mat-option>
            <mat-option value="Garage Door">Garage Door (Garage)</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Action</mat-label>
          <mat-select formControlName="action">
            <mat-option value="Turn On">Turn On</mat-option>
            <mat-option value="Turn Off">Turn Off</mat-option>
            <mat-option value="Dim to 50%">Dim to 50%</mat-option>
            <mat-option value="Open">Open</mat-option>
            <mat-option value="Close">Close</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Time</mat-label>
          <input matInput type="time" formControlName="startTime">
          <mat-hint>When should this schedule run?</mat-hint>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Recurrence</mat-label>
          <mat-select formControlName="recurrence">
            <mat-option value="daily">Every day</mat-option>
            <mat-option value="weekdays">Weekdays (Mon–Fri)</mat-option>
            <mat-option value="weekends">Weekends (Sat–Sun)</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!form.valid" (click)="submit()">
        {{ data.schedule ? 'Save Changes' : 'Add Schedule' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ScheduleDialogComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<ScheduleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { schedule: Schedule | null; defaultTime?: string }
  ) {
    this.form = this.fb.group({
      name: [this.data.schedule?.name ?? '', Validators.required],
      deviceName: [this.data.schedule?.deviceName ?? '', Validators.required],
      action: [this.data.schedule?.action ?? 'Turn On', Validators.required],
      startTime: [this.data.schedule?.startTime ?? (this.data.defaultTime ?? '07:00'), Validators.required],
      recurrence: [this.data.schedule?.recurrence ?? 'weekdays', Validators.required],
    });
  }

  submit() {
    if (this.form.valid) {
      const rec = this.form.value.recurrence;
      const days = rec === 'daily' ? [0,1,2,3,4,5,6] : rec === 'weekdays' ? [0,1,2,3,4] : [5,6];
      this.dialogRef.close({ ...this.form.value, days });
    }
  }
}
