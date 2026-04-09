import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ENERGY_DEVICES, ENERGY_ROOMS } from '../../core/mock-data';
import { EnergyDevice, EnergyRoom } from '../../core/models';

@Component({
  selector: 'app-energy',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatTableModule, MatButtonToggleModule,
    MatSnackBarModule, FormsModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <div class="page-header" style="display:flex;align-items:flex-start;justify-content:space-between;">
        <div>
          <h1>Energy</h1>
          <p class="subtitle">See how much energy your home is using.</p>
        </div>
        <button mat-stroked-button (click)="exportCsv()">
          <mat-icon>download</mat-icon> Export CSV
        </button>
      </div>

      <!-- Summary cards -->
      <div class="stat-cards-row" style="grid-template-columns:repeat(2,1fr);max-width:500px;margin-bottom:24px;">
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(0,137,123,0.1);">
            <mat-icon style="color:#00897B;">bolt</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" style="font-size:28px;">1.8<span style="font-size:16px;font-weight:400;"> kWh</span></div>
            <div class="stat-label">Today's Usage</div>
          </div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(255,179,0,0.1);">
            <mat-icon style="color:#FFB300;">calendar_today</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" style="font-size:28px;">12.4<span style="font-size:16px;font-weight:400;"> kWh</span></div>
            <div class="stat-label">This Week</div>
          </div>
        </mat-card>
      </div>

      <!-- Bar chart -->
      <mat-card style="margin-bottom:24px;">
        <mat-card-header style="padding:16px 16px 0;">
          <mat-card-title style="font-size:16px;">Usage by Room</mat-card-title>
          <div style="margin-left:auto;">
            <mat-button-toggle-group [(ngModel)]="chartView" (ngModelChange)="onChartToggle()">
              <mat-button-toggle value="day">Today</mat-button-toggle>
              <mat-button-toggle value="week">This Week</mat-button-toggle>
            </mat-button-toggle-group>
          </div>
        </mat-card-header>
        <mat-card-content style="padding:16px;">
          <div class="energy-bar-chart">
            <div class="bar-row" *ngFor="let room of rooms">
              <div class="bar-label">{{ room.roomName }}</div>
              <div class="bar-track">
                <div class="bar-fill" [style.width]="getBarWidth(room) + '%'"></div>
              </div>
              <div class="bar-value">{{ chartView === 'day' ? room.todayKwh : room.weekKwh }} kWh</div>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Device breakdown table -->
      <mat-card>
        <mat-card-header style="padding:16px 16px 0;">
          <mat-card-title style="font-size:16px;">Device Breakdown</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="energyDevices" style="width:100%;">
            <ng-container matColumnDef="device">
              <th mat-header-cell *matHeaderCellDef>Device</th>
              <td mat-cell *matCellDef="let row">{{ row.deviceName }}</td>
            </ng-container>
            <ng-container matColumnDef="room">
              <th mat-header-cell *matHeaderCellDef>Room</th>
              <td mat-cell *matCellDef="let row">{{ row.room }}</td>
            </ng-container>
            <ng-container matColumnDef="wattage">
              <th mat-header-cell *matHeaderCellDef>Wattage</th>
              <td mat-cell *matCellDef="let row">{{ row.wattage }} W</td>
            </ng-container>
            <ng-container matColumnDef="today">
              <th mat-header-cell *matHeaderCellDef>Today's Usage</th>
              <td mat-cell *matCellDef="let row" style="min-width:180px;">
                <div style="display:flex;align-items:center;gap:12px;">
                  <mat-progress-bar mode="determinate" [value]="row.todayKwh / 0.7 * 100" color="primary" style="flex:1;border-radius:4px;"></mat-progress-bar>
                  <span style="font-size:13px;font-weight:500;color:#00695C;white-space:nowrap;">{{ row.todayKwh }} kWh</span>
                </div>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="displayedCols"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedCols;"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `,
})
export class EnergyComponent implements OnInit {
  loading = true;
  chartView = 'day';
  energyDevices: EnergyDevice[] = [];
  rooms: EnergyRoom[] = [];
  displayedCols = ['device', 'room', 'wattage', 'today'];

  constructor(private snackBar: MatSnackBar) {}

  ngOnInit() {
    setTimeout(() => {
      this.energyDevices = ENERGY_DEVICES;
      this.rooms = ENERGY_ROOMS;
      this.loading = false;
    }, 600);
  }

  onChartToggle() {}

  getBarWidth(room: EnergyRoom): number {
    const values = this.rooms.map(r => this.chartView === 'day' ? r.todayKwh : r.weekKwh);
    const max = Math.max(...values);
    const val = this.chartView === 'day' ? room.todayKwh : room.weekKwh;
    return max > 0 ? (val / max) * 100 : 0;
  }

  exportCsv() {
    this.snackBar.open('Energy data exported ✓', '', { duration: 2000 });
  }
}
