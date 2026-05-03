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
import { EnergyDevice, EnergyRoom } from '../../core/models';
import { EnergyService } from '../../core/energy.service';

type EnergyPeriod = 'day' | 'week';

interface RoomEnergySummary extends EnergyRoom {
  deviceCount: number;
}

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
      <div class="page-header energy-header">
        <div>
          <h1>Energy Dashboard</h1>
          <p class="subtitle">Estimated consumption by device, room and household.</p>
        </div>
        <div class="energy-actions">
          <mat-button-toggle-group [(ngModel)]="selectedPeriod" aria-label="Energy period">
            <mat-button-toggle value="day">Today</mat-button-toggle>
            <mat-button-toggle value="week">This Week</mat-button-toggle>
          </mat-button-toggle-group>
          <button mat-stroked-button (click)="exportCsv()">
            <mat-icon>download</mat-icon> Export CSV
          </button>
        </div>
      </div>

      <div class="stat-cards-row energy-stat-row">
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(0,137,123,0.1);">
            <mat-icon style="color:#00897B;">bolt</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" style="font-size:28px;">{{ formatKwh(totalForPeriod) }}<span style="font-size:16px;font-weight:400;"> kWh</span></div>
            <div class="stat-label">{{ selectedPeriod === 'day' ? "Today's Usage" : 'Weekly Usage' }}</div>
          </div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(79,70,229,0.1);">
            <mat-icon style="color:#4F46E5;">home</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" style="font-size:28px;">{{ rooms.length }}</div>
            <div class="stat-label">Rooms Tracked</div>
          </div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(249,115,22,0.1);">
            <mat-icon style="color:#F97316;">devices</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" style="font-size:28px;">{{ energyDevices.length }}</div>
            <div class="stat-label">Devices Estimated</div>
          </div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(16,185,129,0.1);">
            <mat-icon style="color:#10B981;">trending_up</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value" style="font-size:28px;">{{ formatKwh(averagePerDevice) }}<span style="font-size:16px;font-weight:400;"> kWh</span></div>
            <div class="stat-label">Avg. per Device</div>
          </div>
        </mat-card>
      </div>

      <mat-card style="margin-bottom:24px;">
        <mat-card-header style="padding:16px 16px 0;">
          <mat-card-title style="font-size:16px;">Usage by Room</mat-card-title>
        </mat-card-header>
        <mat-card-content style="padding:16px;">
          <div class="energy-bar-chart">
            <div class="bar-row" *ngFor="let room of rooms">
              <div class="bar-label">{{ room.roomName }}<span>{{ room.deviceCount }} devices</span></div>
              <div class="bar-track">
                <div class="bar-fill" [style.width]="getBarWidth(room) + '%'"></div>
              </div>
              <div class="bar-value">{{ formatKwh(getRoomUsage(room)) }} kWh</div>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

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
            <ng-container matColumnDef="usage">
              <th mat-header-cell *matHeaderCellDef>{{ selectedPeriod === 'day' ? "Today's Usage" : 'Weekly Usage' }}</th>
              <td mat-cell *matCellDef="let row" style="min-width:180px;">
                <div style="display:flex;align-items:center;gap:12px;">
                  <mat-progress-bar mode="determinate" [value]="getDeviceBarValue(row)" color="primary" style="flex:1;border-radius:4px;"></mat-progress-bar>
                  <span style="font-size:13px;font-weight:500;color:#00695C;white-space:nowrap;">{{ formatKwh(getDeviceUsage(row)) }} kWh</span>
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
  selectedPeriod: EnergyPeriod = 'day';
  energyDevices: EnergyDevice[] = [];
  rooms: RoomEnergySummary[] = [];
  displayedCols = ['device', 'room', 'wattage', 'usage'];

  constructor(
    private snackBar: MatSnackBar,
    private energyService: EnergyService,
  ) {}

  ngOnInit() {
    this.energyService.getDeviceEnergy().subscribe({
      next: energyDevices => {
        this.energyDevices = energyDevices;
        this.rooms = this.aggregateRooms(this.energyDevices);
        this.loading = false;
      },
      error: () => {
        this.energyDevices = [];
        this.rooms = [];
        this.loading = false;
        this.snackBar.open('Failed to load energy data.', '', { duration: 2500 });
      }
    });
  }

  get totalForPeriod(): number {
    return this.energyDevices.reduce((sum, device) => sum + this.getDeviceUsage(device), 0);
  }

  get averagePerDevice(): number {
    return this.energyDevices.length ? this.totalForPeriod / this.energyDevices.length : 0;
  }

  getBarWidth(room: RoomEnergySummary): number {
    const values = this.rooms.map(r => this.getRoomUsage(r));
    const max = Math.max(...values);
    const val = this.getRoomUsage(room);
    return max > 0 ? (val / max) * 100 : 0;
  }

  getRoomUsage(room: EnergyRoom): number {
    return this.selectedPeriod === 'day' ? room.todayKwh : room.weekKwh;
  }

  getDeviceUsage(device: EnergyDevice): number {
    return this.selectedPeriod === 'day' ? device.todayKwh : device.weekKwh;
  }

  getDeviceBarValue(device: EnergyDevice): number {
    const max = Math.max(...this.energyDevices.map(d => this.getDeviceUsage(d)));
    return max > 0 ? (this.getDeviceUsage(device) / max) * 100 : 0;
  }

  formatKwh(value: number): string {
    return value.toFixed(value >= 10 ? 1 : 2).replace(/\.?0+$/, '');
  }

  exportCsv(): void {
    this.energyService.exportCsv();
  }

  private aggregateRooms(devices: EnergyDevice[]): RoomEnergySummary[] {
    const summaries = new Map<string, RoomEnergySummary>();

    devices.forEach(device => {
      const summary = summaries.get(device.room) ?? {
        roomName: device.room,
        todayKwh: 0,
        weekKwh: 0,
        deviceCount: 0,
      };

      summary.todayKwh += device.todayKwh;
      summary.weekKwh += device.weekKwh;
      summary.deviceCount += 1;
      summaries.set(device.room, summary);
    });

    return Array.from(summaries.values()).map(summary => ({
      ...summary,
      todayKwh: this.roundKwh(summary.todayKwh),
      weekKwh: this.roundKwh(summary.weekKwh),
    }));
  }

  private roundKwh(value: number): number {
    return Math.round(value * 100) / 100;
  }
}
