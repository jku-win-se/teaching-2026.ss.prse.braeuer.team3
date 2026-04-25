import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { ScheduleDto, ScheduleRequest, DAYS } from '../../core/models';
import { ScheduleService } from '../../core/schedule.service';
import { ScheduleDialogComponent } from './schedule-dialog.component';

@Component({
  selector: 'app-schedules',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatDialogModule, MatSnackBarModule,
    MatSlideToggleModule, MatChipsModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>

    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Schedules</h1>
        <p class="subtitle">Automate your devices with recurring time-based schedules.</p>
      </div>

      <div *ngIf="schedules.length === 0" class="empty-state">
        <mat-icon>schedule</mat-icon>
        <p>No schedules yet. Click <strong>+</strong> to create one.</p>
      </div>

      <div class="schedule-list">
        <mat-card *ngFor="let sched of schedules" class="schedule-card">
          <mat-card-content>
            <div class="schedule-row">
              <div class="schedule-info">
                <div class="schedule-name">{{ sched.name }}</div>
                <div class="schedule-sub">
                  <mat-icon class="inline-icon">devices</mat-icon>
                  {{ sched.deviceName }} · {{ sched.roomName }}
                </div>
                <div class="schedule-sub">
                  <mat-icon class="inline-icon">access_time</mat-icon>
                  {{ formatTime(sched.hour, sched.minute) }} &nbsp;|&nbsp;
                  <mat-icon class="inline-icon">event_repeat</mat-icon>
                  {{ formatDays(sched.daysOfWeek) }}
                </div>
                <div class="schedule-sub">
                  <mat-icon class="inline-icon">bolt</mat-icon>
                  {{ formatAction(sched.actionPayload) }}
                </div>
              </div>
              <div class="schedule-actions">
                <mat-slide-toggle
                  [checked]="sched.enabled"
                  color="primary"
                  (change)="toggleEnabled(sched)">
                </mat-slide-toggle>
                <button mat-icon-button (click)="editSchedule(sched)" title="Edit">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button color="warn" (click)="deleteSchedule(sched)" title="Delete">
                  <mat-icon>delete</mat-icon>
                </button>
              </div>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </div>

    <div class="fab-container">
      <button mat-fab color="primary" (click)="openAddDialog()">
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .schedule-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-top: 16px;
    }
    .schedule-card {
      border-radius: 12px;
    }
    .schedule-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    .schedule-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      flex: 1;
    }
    .schedule-name {
      font-size: 16px;
      font-weight: 600;
    }
    .schedule-sub {
      font-size: 13px;
      color: rgba(0,0,0,.6);
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .inline-icon {
      font-size: 15px;
      height: 15px;
      width: 15px;
      line-height: 15px;
    }
    .schedule-actions {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: rgba(0,0,0,.4);
      gap: 8px;
    }
    .empty-state mat-icon {
      font-size: 48px;
      height: 48px;
      width: 48px;
    }
  `],
})
export class SchedulesComponent implements OnInit {
  loading = true;
  schedules: ScheduleDto[] = [];

  private readonly DAY_ABBR = Object.fromEntries(DAYS.map(d => [d.value, d.label]));

  constructor(
    private scheduleService: ScheduleService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadSchedules();
  }

  loadSchedules(): void {
    this.loading = true;
    this.scheduleService.getSchedules().subscribe({
      next: schedules => {
        this.schedules = schedules;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  formatTime(hour: number, minute: number): string {
    return `${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`;
  }

  formatDays(daysOfWeek: string[]): string {
    return daysOfWeek.map(d => this.DAY_ABBR[d] ?? d).join(', ');
  }

  formatAction(actionPayload: string): string {
    try {
      const parsed = JSON.parse(actionPayload);
      if (parsed.stateOn === true) { return 'Turn On'; }
      if (parsed.stateOn === false) { return 'Turn Off'; }
    } catch { /* fall through */ }
    return actionPayload;
  }

  toggleEnabled(sched: ScheduleDto): void {
    this.scheduleService.setEnabled(sched.id, !sched.enabled).subscribe({
      next: updated => {
        this.schedules = this.schedules.map(s => s.id === updated.id ? updated : s);
      },
      error: () => {
        this.snackBar.open('Failed to update schedule', '', { duration: 2000 });
      }
    });
  }

  openAddDialog(): void {
    const ref = this.dialog.open(ScheduleDialogComponent, {
      width: '480px',
      data: { schedule: null }
    });
    ref.afterClosed().subscribe((req: ScheduleRequest | undefined) => {
      if (req) {
        this.scheduleService.createSchedule(req).subscribe({
          next: created => {
            this.schedules = [...this.schedules, created];
            this.snackBar.open('Schedule created ✓', '', { duration: 2000 });
          },
          error: () => {
            this.snackBar.open('Failed to create schedule', '', { duration: 2000 });
          }
        });
      }
    });
  }

  editSchedule(sched: ScheduleDto): void {
    const ref = this.dialog.open(ScheduleDialogComponent, {
      width: '480px',
      data: { schedule: sched }
    });
    ref.afterClosed().subscribe((req: ScheduleRequest | undefined) => {
      if (req) {
        this.scheduleService.updateSchedule(sched.id, req).subscribe({
          next: updated => {
            this.schedules = this.schedules.map(s => s.id === updated.id ? updated : s);
            this.snackBar.open('Schedule updated ✓', '', { duration: 2000 });
          },
          error: () => {
            this.snackBar.open('Failed to update schedule', '', { duration: 2000 });
          }
        });
      }
    });
  }

  deleteSchedule(sched: ScheduleDto): void {
    this.scheduleService.deleteSchedule(sched.id).subscribe({
      next: () => {
        this.schedules = this.schedules.filter(s => s.id !== sched.id);
        this.snackBar.open('Schedule deleted', '', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Failed to delete schedule', '', { duration: 2000 });
      }
    });
  }
}
