import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SCHEDULES } from '../../core/mock-data';
import { Schedule } from '../../core/models';
import { ScheduleDialogComponent } from './schedule-dialog.component';

@Component({
  selector: 'app-schedules',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatDialogModule, MatSnackBarModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Schedules</h1>
        <p class="subtitle">Plan when your devices turn on and off automatically throughout the week.</p>
      </div>

      <!-- Weekly Grid -->
      <div class="schedule-grid-wrapper">
        <div class="schedule-grid">
          <!-- Header row -->
          <div class="grid-header time-header"></div>
          <div class="grid-header" *ngFor="let day of days">{{ day }}</div>

          <!-- Time rows -->
          <ng-container *ngFor="let hour of hours; let hi = index">
            <div class="time-cell">{{ hour }}</div>
            <div
              class="day-cell"
              *ngFor="let day of daysIdx"
              (click)="onCellClick(hi, day)">
              <ng-container *ngFor="let sched of getSchedulesForCell(hi, day)">
                <div
                  class="schedule-block"
                  [style.background]="sched.color"
                  (click)="$event.stopPropagation(); editSchedule(sched)">
                  {{ sched.name }}
                </div>
              </ng-container>
            </div>
          </ng-container>
        </div>
      </div>
    </div>

    <div class="fab-container">
      <button mat-fab color="primary" (click)="openAddDialog()">
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
})
export class SchedulesComponent implements OnInit {
  loading = true;
  schedules: Schedule[] = [];
  days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  daysIdx = [0, 1, 2, 3, 4, 5, 6];
  hours: string[] = [];

  constructor(private dialog: MatDialog, private snackBar: MatSnackBar) {
    for (let h = 6; h <= 23; h++) {
      this.hours.push(`${h.toString().padStart(2, '0')}:00`);
    }
  }

  ngOnInit() {
    setTimeout(() => {
      this.schedules = SCHEDULES.map(s => ({ ...s }));
      this.loading = false;
    }, 600);
  }

  getScheduleHourIndex(sched: Schedule): number {
    const hour = parseInt(sched.startTime.split(':')[0], 10);
    return hour - 6;
  }

  getSchedulesForCell(hourIdx: number, dayIdx: number): Schedule[] {
    return this.schedules.filter(s =>
      this.getScheduleHourIndex(s) === hourIdx &&
      s.days.includes(dayIdx)
    );
  }

  onCellClick(hourIdx: number, _dayIdx: number) {
    const hour = hourIdx + 6;
    this.openAddDialog(`${hour.toString().padStart(2, '0')}:00`);
  }

  openAddDialog(defaultTime?: string) {
    const ref = this.dialog.open(ScheduleDialogComponent, {
      width: '480px',
      data: { schedule: null, defaultTime }
    });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.schedules = [...this.schedules, {
          id: 'sc_' + Date.now(),
          name: result.name,
          deviceId: result.deviceId || 'd1',
          deviceName: result.deviceName || 'Device',
          action: result.action || 'Turn On',
          days: result.days || [0, 1, 2, 3, 4],
          startTime: result.startTime || '07:00',
          color: '#00897B',
          recurrence: result.recurrence || 'weekdays',
        }];
        this.snackBar.open('Schedule created ✓', '', { duration: 2000 });
      }
    });
  }

  editSchedule(sched: Schedule) {
    const ref = this.dialog.open(ScheduleDialogComponent, {
      width: '480px',
      data: { schedule: sched }
    });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.schedules = this.schedules.map(s => s.id === sched.id ? { ...s, ...result } : s);
        this.snackBar.open('Schedule updated ✓', '', { duration: 2000 });
      }
    });
  }
}
