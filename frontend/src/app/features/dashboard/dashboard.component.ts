import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { FormsModule } from '@angular/forms';
import { DEVICES, ROOMS, SCENES, ACTIVITY_LOG } from '../../core/mock-data';
import { Device, Scene, ActivityEntry } from '../../core/models';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatSlideToggleModule,
    MatSnackBarModule, MatProgressBarModule, MatDatepickerModule, MatFormFieldModule,
    MatInputModule, MatNativeDateModule, FormsModule,
  ],
  template: `
    <div *ngIf="loading">
      <mat-progress-bar mode="indeterminate"></mat-progress-bar>
    </div>
    <div class="page-container" *ngIf="!loading">
      <!-- Greeting -->
      <div class="greeting-section" style="margin-bottom:28px;">
        <h1>{{ greeting }}, {{ auth.currentUser?.name }} 👋</h1>
        <p>Here's what's happening in your home today.</p>
      </div>

      <!-- Summary Cards -->
      <div class="stat-cards-row">
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(79,70,229,0.1);">
            <mat-icon style="color:#4F46E5;">devices</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">12</div>
            <div class="stat-label">Total Devices</div>
          </div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(16,185,129,0.1);">
            <mat-icon style="color:#10B981;">check_circle</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">7</div>
            <div class="stat-label">Active Now</div>
          </div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(139,92,246,0.1);">
            <mat-icon style="color:#8B5CF6;">rule</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">4</div>
            <div class="stat-label">Rules Running</div>
          </div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(249,115,22,0.1);">
            <mat-icon style="color:#F97316;">notifications</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">2</div>
            <div class="stat-label">Open Alerts</div>
          </div>
        </mat-card>
      </div>

      <!-- Quick Controls -->
      <div class="section-title">Quick Controls</div>
      <div class="quick-controls-grid" style="margin-bottom:28px;">
        <mat-card
          *ngFor="let device of quickDevices"
          class="quick-control-card"
          [class.device-on]="device.state.on"
          (click)="toggleDevice(device)">
          <div class="device-icon-wrap">
            <mat-icon>{{ device.icon }}</mat-icon>
          </div>
          <p class="device-name">{{ device.name }}</p>
          <p class="device-room">{{ getRoomName(device.roomId) }}</p>
          <span class="device-status" [style.color]="device.state.on ? '#4F46E5' : '#94A3B8'">
            {{ device.state.on ? '● ON' : '○ OFF' }}
          </span>
        </mat-card>
      </div>

      <!-- My Scenes -->
      <div class="section-title">My Scenes</div>
      <div class="scene-chips-row" style="margin-bottom:32px;">
        <div class="scene-chip" *ngFor="let scene of scenes" (click)="activateScene(scene)">
          <mat-icon>{{ scene.icon }}</mat-icon>
          {{ scene.name }}
        </div>
      </div>

      <!-- Recent Activity -->
      <div class="section-title">Recent Activity</div>
      <mat-card style="margin-bottom:24px;">
        <mat-card-content>
          <div class="activity-list">
            <div class="activity-item" *ngFor="let entry of recentActivity">
              <div class="activity-icon" [style.background]="getDeviceIconBg(entry.deviceType)">
                <mat-icon [style.color]="getDeviceIconColor(entry.deviceType)">{{ getDeviceIcon(entry.deviceType) }}</mat-icon>
              </div>
              <div class="activity-text">
                <p class="activity-desc">{{ entry.description }}</p>
                <p class="activity-time">{{ relativeTime(entry.timestamp) }}</p>
              </div>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Vacation Mode -->
      <div class="vacation-banner">
        <mat-icon class="vacation-icon">{{ vacationActive ? 'beach_access' : 'flight_takeoff' }}</mat-icon>
        <div class="vacation-text">
          <h3>Vacation Mode</h3>
          <p *ngIf="vacationActive">🏖️ Vacation mode is active until {{ vacationEnd | date:'MMMM d' }}</p>
          <p *ngIf="!vacationActive">Away for a while? Activate vacation mode to keep your home secure.</p>
        </div>
        <mat-slide-toggle [(ngModel)]="vacationActive" color="accent" (change)="onVacationToggle($event.checked)"></mat-slide-toggle>
      </div>
    </div>
  `,
})
export class DashboardComponent implements OnInit {
  loading = true;
  greeting = '';
  vacationActive = false;
  vacationEnd = new Date(Date.now() + 14 * 86400000);

  quickDevices: Device[] = [];
  scenes = SCENES;
  recentActivity: ActivityEntry[] = [];

  constructor(private snackBar: MatSnackBar, public auth: AuthService) {}

  ngOnInit() {
    const hour = new Date().getHours();
    this.greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
    setTimeout(() => {
      this.loading = false;
      this.quickDevices = DEVICES.filter(d => ['d1','d2','d9','d11'].includes(d.id));
      this.recentActivity = ACTIVITY_LOG.slice(0, 5);
    }, 600);
  }

  getRoomName(roomId: string) {
    return ROOMS.find(r => r.id === roomId)?.name ?? '';
  }

  toggleDevice(device: Device) {
    device.state = { ...device.state, on: !device.state.on };
    this.snackBar.open(`${device.name} turned ${device.state.on ? 'on' : 'off'} ✓`, '', { duration: 2000 });
  }

  activateScene(scene: Scene) {
    this.snackBar.open(`${scene.name} activated ✓`, '', { duration: 2000 });
  }

  onVacationToggle(active: boolean) {
    this.snackBar.open(active ? 'Vacation mode enabled ✓' : 'Vacation mode disabled', '', { duration: 2000 });
  }

  relativeTime(date: Date): string {
    const diff = Math.floor((Date.now() - date.getTime()) / 60000);
    if (diff < 1) return 'just now';
    if (diff < 60) return `${diff} min ago`;
    const h = Math.floor(diff / 60);
    return `${h}h ago`;
  }

  getDeviceIcon(type: string): string {
    const map: Record<string, string> = { switch: 'lightbulb', dimmer: 'lightbulb', thermostat: 'thermostat', sensor: 'sensors', cover: 'blinds' };
    return map[type] ?? 'devices';
  }

  getDeviceIconBg(type: string): string {
    const map: Record<string, string> = { switch: 'rgba(249,115,22,0.1)', dimmer: 'rgba(249,115,22,0.1)', thermostat: 'rgba(139,92,246,0.1)', sensor: 'rgba(79,70,229,0.1)', cover: 'rgba(16,185,129,0.1)' };
    return map[type] ?? 'var(--bg)';
  }

  getDeviceIconColor(type: string): string {
    const map: Record<string, string> = { switch: '#F97316', dimmer: '#F97316', thermostat: '#8B5CF6', sensor: '#4F46E5', cover: '#10B981' };
    return map[type] ?? '#94A3B8';
  }
}
