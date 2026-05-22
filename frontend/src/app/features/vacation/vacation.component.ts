import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { VacationModeDto, VacationModeRequest } from '../../core/models';
import { VacationModeService } from '../../core/vacation-mode.service';
import { VacationDialogComponent } from './vacation-dialog.component';

@Component({
  selector: 'app-vacation',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatChipsModule, MatDialogModule, MatSnackBarModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>

    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Vacation Mode</h1>
        <p class="subtitle">Automatically apply a schedule while you're away.</p>
      </div>

      <div *ngIf="vacationModes.length === 0" class="empty-state">
        <mat-icon>beach_access</mat-icon>
        <p>No vacation mode configured. Click <strong>+</strong> to set one up.</p>
      </div>

      <div class="vacation-list">
        <mat-card *ngFor="let vm of vacationModes" class="vacation-card">
          <mat-card-content>
            <div class="vacation-row">
              <div class="vacation-info">
                <div class="vacation-name">{{ vm.name }}</div>

                <div class="vacation-sub">
                  <mat-icon class="inline-icon">schedule</mat-icon>
                  {{ vm.scheduleName }}
                  &mdash;
                  <span [style.color]="vm.action === 'ENABLE' ? '#10B981' : '#EF4444'">
                    {{ vm.action === 'ENABLE' ? 'Enable' : 'Disable' }}
                  </span>
                </div>

                <div class="vacation-sub">
                  <mat-icon class="inline-icon">date_range</mat-icon>
                  {{ vm.startDate }} &ndash; {{ vm.endDate }}
                </div>

                <div style="margin-top:6px;">
                  <mat-chip-set>
                    <mat-chip
                      *ngIf="vm.active"
                      style="background:#D1FAE5;color:#065F46;font-weight:600;">
                      <mat-icon style="font-size:14px;height:14px;width:14px;margin-right:4px;">check_circle</mat-icon>
                      Active
                    </mat-chip>
                    <mat-chip
                      *ngIf="!vm.active && !vm.deactivated && !isPast(vm)"
                      style="background:#FEF3C7;color:#92400E;font-weight:600;">
                      <mat-icon style="font-size:14px;height:14px;width:14px;margin-right:4px;">upcoming</mat-icon>
                      Upcoming
                    </mat-chip>
                    <mat-chip
                      *ngIf="vm.deactivated"
                      style="background:#F1F5F9;color:#64748B;font-weight:600;">
                      <mat-icon style="font-size:14px;height:14px;width:14px;margin-right:4px;">cancel</mat-icon>
                      Deactivated
                    </mat-chip>
                  </mat-chip-set>
                </div>
              </div>

              <div class="vacation-actions">
                <button
                  *ngIf="!vm.deactivated"
                  mat-stroked-button color="warn"
                  (click)="deactivate(vm)"
                  title="Deactivate early">
                  <mat-icon>stop_circle</mat-icon>
                  Deactivate
                </button>
                <button mat-icon-button color="warn" (click)="delete(vm)" title="Delete">
                  <mat-icon>delete</mat-icon>
                </button>
              </div>
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <div class="fab-container">
        <button
          mat-fab color="primary"
          (click)="openAddDialog()"
          title="Add vacation mode">
          <mat-icon>add</mat-icon>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .vacation-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-top: 16px;
    }
    .vacation-card {
      border-radius: 12px;
    }
    .vacation-row {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 16px;
    }
    .vacation-info {
      display: flex;
      flex-direction: column;
      gap: 4px;
      flex: 1;
    }
    .vacation-name {
      font-size: 16px;
      font-weight: 600;
    }
    .vacation-sub {
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
    .vacation-actions {
      display: flex;
      align-items: center;
      gap: 4px;
      flex-shrink: 0;
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
export class VacationComponent implements OnInit {
  loading = true;
  vacationModes: VacationModeDto[] = [];

  constructor(
    private vacationModeService: VacationModeService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.vacationModeService.getVacationModes().subscribe({
      next: modes => {
        this.vacationModes = modes;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  isPast(vm: VacationModeDto): boolean {
    return vm.endDate < new Date().toISOString().slice(0, 10);
  }

  openAddDialog(): void {
    const ref = this.dialog.open(VacationDialogComponent, { width: '460px' });
    ref.afterClosed().subscribe((req: VacationModeRequest | undefined) => {
      if (!req) { return; }
      this.vacationModeService.createVacationMode(req).subscribe({
        next: created => {
          this.vacationModes = [...this.vacationModes, created];
          this.snackBar.open('Vacation mode created', '', { duration: 2000 });
        },
        error: () => {
          this.snackBar.open('Failed to create vacation mode', '', { duration: 3000 });
        }
      });
    });
  }

  deactivate(vm: VacationModeDto): void {
    this.vacationModeService.deactivate(vm.id).subscribe({
      next: updated => {
        this.vacationModes = this.vacationModes.map(v => v.id === updated.id ? updated : v);
        this.snackBar.open('Vacation mode deactivated', '', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Failed to deactivate vacation mode', '', { duration: 2000 });
      }
    });
  }

  delete(vm: VacationModeDto): void {
    this.vacationModeService.deleteVacationMode(vm.id).subscribe({
      next: () => {
        this.vacationModes = this.vacationModes.filter(v => v.id !== vm.id);
        this.snackBar.open('Vacation mode deleted', '', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Failed to delete vacation mode', '', { duration: 2000 });
      }
    });
  }
}
