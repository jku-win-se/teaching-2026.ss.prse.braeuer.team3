import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, forkJoin, of } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ActivityLogService } from '../../core/activity-log.service';
import { RealtimeService } from '../../core/realtime.service';
import { DeviceService, DeviceDto } from '../../core/device.service';
import { RoomService, RoomDto } from '../../core/room.service';
import { ActivityLogDto } from '../../core/models';

@Component({
  selector: 'app-log',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatTableModule, MatPaginatorModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatSnackBarModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <div class="page-header" style="display:flex;align-items:flex-start;justify-content:space-between;">
        <div>
          <h1>Activity Log</h1>
          <p class="subtitle">A full history of everything that happened in your home.</p>
        </div>
        <button mat-stroked-button data-testid="btn-export-csv"
                (click)="exportCsv()">
          <mat-icon>download</mat-icon> Export CSV
        </button>
      </div>

      <!-- Filter bar -->
      <div style="display:flex;gap:16px;align-items:center;margin-bottom:16px;flex-wrap:wrap;">
        <mat-form-field appearance="outline" style="min-width:180px;">
          <mat-label>From</mat-label>
          <input matInput type="datetime-local" data-testid="filter-from"
                 [(ngModel)]="filterFrom">
        </mat-form-field>
        <mat-form-field appearance="outline" style="min-width:180px;">
          <mat-label>To</mat-label>
          <input matInput type="datetime-local" data-testid="filter-to"
                 [(ngModel)]="filterTo">
        </mat-form-field>
        <mat-form-field appearance="outline" style="min-width:180px;">
          <mat-label>Device</mat-label>
          <mat-select data-testid="filter-device" [(ngModel)]="filterDeviceId">
            <mat-option [value]="null">All devices</mat-option>
            <mat-option *ngFor="let d of allDevices" [value]="d.id">{{ d.name }}</mat-option>
          </mat-select>
        </mat-form-field>
        <button mat-flat-button color="primary" data-testid="btn-apply-filter"
                (click)="applyFilter()">
          Apply
        </button>
        <button mat-stroked-button data-testid="btn-clear-filter"
                (click)="clearFilter()">
          Clear
        </button>
      </div>

      <!-- Table -->
      <mat-card>
        <mat-card-content style="padding:0;">
          <table mat-table [dataSource]="entries" style="width:100%;">

            <ng-container matColumnDef="timestamp">
              <th mat-header-cell *matHeaderCellDef>Time</th>
              <td mat-cell *matCellDef="let row" style="white-space:nowrap;color:#757575;font-size:13px;">
                {{ row.timestamp | date:'MMM d, H:mm' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="device">
              <th mat-header-cell *matHeaderCellDef>Device</th>
              <td mat-cell *matCellDef="let row" style="font-weight:500;">{{ row.deviceName }}</td>
            </ng-container>

            <ng-container matColumnDef="room">
              <th mat-header-cell *matHeaderCellDef>Room</th>
              <td mat-cell *matCellDef="let row" style="color:#757575;">{{ row.roomName }}</td>
            </ng-container>

            <ng-container matColumnDef="action">
              <th mat-header-cell *matHeaderCellDef>What happened</th>
              <td mat-cell *matCellDef="let row">{{ row.action }}</td>
            </ng-container>

            <ng-container matColumnDef="actor">
              <th mat-header-cell *matHeaderCellDef>Actor</th>
              <td mat-cell *matCellDef="let row" style="color:#757575;">{{ row.actorName }}</td>
            </ng-container>

            <ng-container matColumnDef="delete">
              <th mat-header-cell *matHeaderCellDef style="width:48px;"></th>
              <td mat-cell *matCellDef="let row">
                <button mat-icon-button color="warn" data-testid="btn-delete-log"
                        (click)="deleteEntry(row)" title="Delete entry">
                  <mat-icon>delete</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedCols"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedCols;" style="cursor:default;"></tr>
          </table>

          <div *ngIf="entries.length === 0" class="empty-state" style="padding:32px;text-align:center;">
            <mat-icon class="empty-icon">history</mat-icon>
            <h3>No entries found</h3>
            <p>Device state changes will appear here.</p>
          </div>

          <mat-paginator
            data-testid="log-paginator"
            [length]="totalElements"
            [pageSize]="pageSize"
            [pageSizeOptions]="[10, 20, 50]"
            (page)="onPage($event)"
            showFirstLastButtons>
          </mat-paginator>
        </mat-card-content>
      </mat-card>
    </div>
  `,
})
export class LogComponent implements OnInit, OnDestroy {
  loading = true;
  entries: ActivityLogDto[] = [];
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  allDevices: DeviceDto[] = [];

  filterFrom = '';
  filterTo = '';
  filterDeviceId: number | null = null;

  displayedCols = ['timestamp', 'device', 'room', 'action', 'actor', 'delete'];

  private realtimeSub?: Subscription;

  constructor(
    private activityLogService: ActivityLogService,
    private realtimeService: RealtimeService,
    private deviceService: DeviceService,
    private roomService: RoomService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.loadDevices();
    this.loadLogs();
    // On new real-time entry, reload current page so the chronological list stays accurate.
    // New entries appear at the end (ASC order), so the last page is affected most.
    this.realtimeSub = this.realtimeService.activityLogUpdates$.subscribe(() => {
      this.loadLogs();
    });
  }

  ngOnDestroy(): void {
    this.realtimeSub?.unsubscribe();
  }

  loadDevices(): void {
    this.roomService.getRooms().pipe(
      switchMap((rooms: RoomDto[]) => {
        if (rooms.length === 0) { return of<DeviceDto[][]>([]); }
        return forkJoin(rooms.map(r => this.deviceService.getDevices(r.id).pipe(catchError(() => of<DeviceDto[]>([])))));
      }),
      catchError(() => of<DeviceDto[][]>([]))
    ).subscribe((devicesByRoom: DeviceDto[][]) => {
      this.allDevices = devicesByRoom.flat();
    });
  }

  loadLogs(): void {
    this.loading = true;
    const from = this.filterFrom ? new Date(this.filterFrom).toISOString() : undefined;
    const to = this.filterTo ? new Date(this.filterTo).toISOString() : undefined;
    const deviceId = this.filterDeviceId ?? undefined;
    this.activityLogService.getLogs(this.pageIndex, this.pageSize, from, to, deviceId).subscribe({
      next: page => {
        this.entries = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load activity log.', '', { duration: 3000 });
      },
    });
  }

  applyFilter(): void {
    this.pageIndex = 0;
    this.loadLogs();
  }

  clearFilter(): void {
    this.filterFrom = '';
    this.filterTo = '';
    this.filterDeviceId = null;
    this.pageIndex = 0;
    this.loadLogs();
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadLogs();
  }

  deleteEntry(entry: ActivityLogDto): void {
    this.activityLogService.deleteLog(entry.id).subscribe({
      next: () => {
        this.entries = this.entries.filter(e => e.id !== entry.id);
        this.totalElements--;
        this.snackBar.open('Entry deleted.', '', { duration: 2000 });
      },
      error: () => {
        this.snackBar.open('Failed to delete entry.', '', { duration: 3000 });
      },
    });
  }

  exportCsv(): void {
    this.activityLogService.exportCsv();
  }
}
