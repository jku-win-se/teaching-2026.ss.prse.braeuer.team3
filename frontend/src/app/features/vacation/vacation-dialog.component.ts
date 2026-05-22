import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ScheduleDto } from '../../core/models';
import { ScheduleService } from '../../core/schedule.service';

@Component({
  selector: 'app-vacation-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatProgressSpinnerModule,
    FormsModule, ReactiveFormsModule,
  ],
  template: `
    <h2 mat-dialog-title>Add Vacation Mode</h2>
    <mat-dialog-content style="min-width:400px;padding-top:8px;">

      <div *ngIf="loading" style="display:flex;justify-content:center;padding:24px;">
        <mat-spinner diameter="40"></mat-spinner>
      </div>

      <form *ngIf="!loading" [formGroup]="form" style="display:flex;flex-direction:column;gap:14px;">

        <mat-form-field appearance="outline">
          <mat-label>Vacation name</mat-label>
          <input matInput formControlName="name" placeholder="e.g. Summer Holiday">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Schedule</mat-label>
          <mat-select formControlName="scheduleId">
            <mat-option *ngFor="let s of schedules" [value]="s.id">
              {{ s.name }} ({{ s.deviceName }})
            </mat-option>
          </mat-select>
          <mat-hint *ngIf="schedules.length === 0">No schedules found — create a schedule first.</mat-hint>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Action</mat-label>
          <mat-select formControlName="action">
            <mat-option value="ENABLE">Enable schedule during vacation</mat-option>
            <mat-option value="DISABLE">Disable schedule during vacation</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Start date</mat-label>
          <input matInput type="date" formControlName="startDate">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>End date</mat-label>
          <input matInput type="date" formControlName="endDate">
          <mat-error *ngIf="form.errors?.['endBeforeStart']">End date must not be before start date.</mat-error>
        </mat-form-field>

      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button
        mat-flat-button color="primary"
        [disabled]="loading || !form.valid || !!form.errors?.['endBeforeStart']"
        (click)="submit()">
        Add Vacation Mode
      </button>
    </mat-dialog-actions>
  `,
})
export class VacationDialogComponent implements OnInit {
  form: FormGroup;
  schedules: ScheduleDto[] = [];
  loading = true;

  constructor(
    private fb: FormBuilder,
    private scheduleService: ScheduleService,
    public dialogRef: MatDialogRef<VacationDialogComponent>,
  ) {
    this.form = this.fb.group({
      name:       ['', Validators.required],
      scheduleId: [null, Validators.required],
      action:     ['ENABLE', Validators.required],
      startDate:  ['', Validators.required],
      endDate:    ['', Validators.required],
    }, { validators: this.endAfterStartValidator });
  }

  ngOnInit(): void {
    this.scheduleService.getSchedules().subscribe({
      next: schedules => {
        this.schedules = schedules;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  private endAfterStartValidator(group: FormGroup): { endBeforeStart: true } | null {
    const start = group.get('startDate')?.value as string;
    const end = group.get('endDate')?.value as string;
    if (start && end && end < start) {
      return { endBeforeStart: true };
    }
    return null;
  }

  submit(): void {
    if (!this.form.valid || this.form.errors?.['endBeforeStart']) { return; }
    const { name, scheduleId, action, startDate, endDate } = this.form.value;
    this.dialogRef.close({ name, scheduleId, action, startDate, endDate });
  }
}
