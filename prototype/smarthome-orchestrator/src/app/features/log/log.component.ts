import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ACTIVITY_LOG } from '../../core/mock-data';
import { ActivityEntry } from '../../core/models';

@Component({
  selector: 'app-log',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatProgressBarModule,
    MatTableModule, MatPaginatorModule, MatFormFieldModule, MatInputModule,
    MatSnackBarModule, FormsModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <div class="page-header" style="display:flex;align-items:flex-start;justify-content:space-between;">
        <div>
          <h1>Activity Log</h1>
          <p class="subtitle">A full history of everything that happened in your home.</p>
        </div>
        <button mat-stroked-button (click)="exportCsv()">
          <mat-icon>download</mat-icon> Export CSV
        </button>
      </div>

      <!-- Filter bar -->
      <div style="display:flex;gap:16px;align-items:center;margin-bottom:16px;flex-wrap:wrap;">
        <mat-form-field appearance="outline" style="flex:1;min-width:200px;max-width:400px;">
          <mat-label>Search activity</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput [(ngModel)]="searchQuery" (ngModelChange)="filterEntries()" placeholder="Search by device, room, description...">
        </mat-form-field>
        <div class="filter-chips">
          <div class="filter-chip" [class.active]="activeFilter === 'all'" (click)="setFilter('all')">All</div>
          <div class="filter-chip" [class.active]="activeFilter === 'user'" (click)="setFilter('user')">By User</div>
          <div class="filter-chip" [class.active]="activeFilter === 'rule'" (click)="setFilter('rule')">By Rule</div>
          <div class="filter-chip" [class.active]="activeFilter === 'sensor'" (click)="setFilter('sensor')">By Sensor</div>
        </div>
      </div>

      <!-- Table -->
      <mat-card>
        <mat-card-content style="padding:0;">
          <table mat-table [dataSource]="pagedEntries" style="width:100%;">

            <ng-container matColumnDef="icon">
              <th mat-header-cell *matHeaderCellDef style="width:48px;"></th>
              <td mat-cell *matCellDef="let row" style="padding:8px 0 8px 16px;">
                <div class="activity-icon" [style.background]="getIconBg(row.deviceType)" style="width:32px;height:32px;">
                  <mat-icon [style.color]="getIconColor(row.deviceType)" style="font-size:16px;width:16px;height:16px;">{{ getDeviceIcon(row.deviceType) }}</mat-icon>
                </div>
              </td>
            </ng-container>

            <ng-container matColumnDef="timestamp">
              <th mat-header-cell *matHeaderCellDef>Time</th>
              <td mat-cell *matCellDef="let row" style="white-space:nowrap;color:#757575;font-size:13px;">
                {{ row.timestamp | date:'MMM d, h:mm a' }}
              </td>
            </ng-container>

            <ng-container matColumnDef="device">
              <th mat-header-cell *matHeaderCellDef>Device</th>
              <td mat-cell *matCellDef="let row" style="font-weight:500;">{{ row.deviceName }}</td>
            </ng-container>

            <ng-container matColumnDef="room">
              <th mat-header-cell *matHeaderCellDef>Room</th>
              <td mat-cell *matCellDef="let row" style="color:#757575;">{{ row.room }}</td>
            </ng-container>

            <ng-container matColumnDef="description">
              <th mat-header-cell *matHeaderCellDef>What happened</th>
              <td mat-cell *matCellDef="let row">{{ row.description }}</td>
            </ng-container>

            <ng-container matColumnDef="triggeredBy">
              <th mat-header-cell *matHeaderCellDef>Triggered by</th>
              <td mat-cell *matCellDef="let row">
                <span style="padding:2px 10px;border-radius:10px;font-size:12px;font-weight:500;"
                  [style.background]="row.triggeredBy === 'Rule' ? 'rgba(66,133,244,0.1)' : row.triggeredBy === 'Sensor' ? 'rgba(0,137,123,0.1)' : row.triggeredBy === 'Schedule' ? 'rgba(156,39,176,0.1)' : '#f5f5f5'"
                  [style.color]="row.triggeredBy === 'Rule' ? '#1976D2' : row.triggeredBy === 'Sensor' ? '#00695C' : row.triggeredBy === 'Schedule' ? '#7B1FA2' : '#616161'">
                  {{ row.triggeredBy }}
                </span>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedCols"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedCols;" style="cursor:default;"></tr>
          </table>

          <div *ngIf="filteredEntries.length === 0" class="empty-state" style="padding:32px;">
            <mat-icon class="empty-icon">search_off</mat-icon>
            <h3>No entries found</h3>
            <p>Try a different search term or filter.</p>
          </div>

          <mat-paginator
            [length]="filteredEntries.length"
            [pageSize]="10"
            [pageSizeOptions]="[5, 10, 20]"
            (page)="onPage($event)"
            showFirstLastButtons>
          </mat-paginator>
        </mat-card-content>
      </mat-card>
    </div>
  `,
})
export class LogComponent implements OnInit {
  loading = true;
  searchQuery = '';
  activeFilter = 'all';
  allEntries: ActivityEntry[] = [];
  filteredEntries: ActivityEntry[] = [];
  pagedEntries: ActivityEntry[] = [];
  displayedCols = ['icon', 'timestamp', 'device', 'room', 'description', 'triggeredBy'];
  private pageIndex = 0;
  private pageSize = 10;

  constructor(private snackBar: MatSnackBar) {}

  ngOnInit() {
    setTimeout(() => {
      this.allEntries = ACTIVITY_LOG;
      this.filteredEntries = [...this.allEntries];
      this.updatePage();
      this.loading = false;
    }, 600);
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
    this.filterEntries();
  }

  filterEntries() {
    this.filteredEntries = this.allEntries.filter(e => {
      const matchesFilter =
        this.activeFilter === 'all' ||
        (this.activeFilter === 'user' && !['Rule', 'Sensor', 'Schedule'].includes(e.triggeredBy)) ||
        (this.activeFilter === 'rule' && e.triggeredBy === 'Rule') ||
        (this.activeFilter === 'sensor' && e.triggeredBy === 'Sensor');
      const q = this.searchQuery.toLowerCase();
      const matchesSearch = !q || e.deviceName.toLowerCase().includes(q) || e.room.toLowerCase().includes(q) || e.description.toLowerCase().includes(q);
      return matchesFilter && matchesSearch;
    });
    this.pageIndex = 0;
    this.updatePage();
  }

  onPage(event: PageEvent) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updatePage();
  }

  updatePage() {
    const start = this.pageIndex * this.pageSize;
    this.pagedEntries = this.filteredEntries.slice(start, start + this.pageSize);
  }

  exportCsv() { this.snackBar.open('Activity log exported ✓', '', { duration: 2000 }); }

  getDeviceIcon(type: string): string {
    const map: Record<string, string> = { switch: 'toggle_on', dimmer: 'lightbulb', thermostat: 'thermostat', sensor: 'sensors', cover: 'blinds' };
    return map[type] ?? 'devices';
  }
  getIconBg(type: string): string {
    const map: Record<string, string> = { switch: 'rgba(255,179,0,0.1)', dimmer: 'rgba(255,179,0,0.1)', thermostat: 'rgba(66,133,244,0.1)', sensor: 'rgba(0,137,123,0.1)', cover: 'rgba(156,39,176,0.1)' };
    return map[type] ?? '#f5f5f5';
  }
  getIconColor(type: string): string {
    const map: Record<string, string> = { switch: '#FFB300', dimmer: '#FFB300', thermostat: '#1976D2', sensor: '#00897B', cover: '#8E24AA' };
    return map[type] ?? '#9e9e9e';
  }
}
