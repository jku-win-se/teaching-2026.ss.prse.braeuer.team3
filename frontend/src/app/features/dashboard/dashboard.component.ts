import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { SCENES, ACTIVITY_LOG } from '../../core/mock-data';
import { Device, Room, Scene, ActivityEntry } from '../../core/models';
import { AuthService } from '../../core/auth.service';
import { RoomService, RoomDto } from '../../core/room.service';
import { DeviceService, DeviceDto } from '../../core/device.service';
import { RealtimeService } from '../../core/realtime.service';
import { toRoom, dtoToDevice, DEVICE_ICON, DEVICE_ICON_BG, DEVICE_ICON_COLOR } from '../../core/device-utils';
import { DeviceCardComponent } from '../../shared/components/device-card/device-card.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatSlideToggleModule,
    MatSnackBarModule, MatProgressBarModule, FormsModule, DeviceCardComponent,
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
        <mat-card class="stat-card" style="cursor:pointer;" (click)="goToRooms()">
          <div class="stat-icon-bg" style="background:rgba(79,70,229,0.1);">
            <mat-icon style="color:#4F46E5;">devices</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ totalDevices }}</div>
            <div class="stat-label">Total Devices</div>
          </div>
        </mat-card>
        <mat-card class="stat-card">
          <div class="stat-icon-bg" style="background:rgba(16,185,129,0.1);">
            <mat-icon style="color:#10B981;">check_circle</mat-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ activeDevices }}</div>
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
      <div *ngIf="quickDevices.length === 0" style="color:rgba(0,0,0,.4);margin-bottom:28px;">
        No devices found.
        <span style="cursor:pointer;color:#4F46E5;" (click)="goToRooms()">Add a device →</span>
      </div>
      <div class="quick-controls-grid" style="margin-bottom:28px;">
        <app-device-card
          *ngFor="let device of quickDevices"
          [device]="device"
          [room]="getRoom(device.roomId)"
          [showMenu]="false"
          (toggled)="onToggle(device, $event)"
          (sliderChanged)="onBrightnessChange(device, $event)"
          (tempChanged)="onTempChange(device, $event)"
          (coverAction)="onCoverAction(device, $event)">
        </app-device-card>
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
export class DashboardComponent implements OnInit, OnDestroy {
  loading = true;
  greeting = '';
  vacationActive = false;
  vacationEnd = new Date(Date.now() + 14 * 86400000);

  quickDevices: Device[] = [];
  rooms: Room[] = [];
  totalDevices = 0;
  activeDevices = 0;

  private allDevices: Device[] = [];
  private realtimeSub: Subscription | null = null;

  scenes = SCENES;
  recentActivity: ActivityEntry[] = [];

  constructor(
    private snackBar: MatSnackBar,
    private router: Router,
    private roomService: RoomService,
    private deviceService: DeviceService,
    private realtimeService: RealtimeService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const hour = new Date().getHours();
    this.greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening';
    this.recentActivity = ACTIVITY_LOG.slice(0, 5);

    this.roomService.getRooms().pipe(
      switchMap((dtos: RoomDto[]) => {
        this.rooms = dtos.map(toRoom);
        if (!dtos.length) { return of([] as Device[][]); }
        return forkJoin(
          dtos.map(dto =>
            this.deviceService.getDevices(dto.id).pipe(
              map((devices: DeviceDto[]) => devices.map(d => dtoToDevice(d, String(dto.id))))
            )
          )
        );
      }),
      map((nested: Device[][]) => nested.flat())
    ).subscribe({
      next: (devices: Device[]) => {
        this.allDevices = devices;
        this.totalDevices = devices.length;
        this.activeDevices = devices.filter(d => d.state.on).length;
        this.quickDevices = devices.slice(0, 8);
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });

    this.realtimeService.connect();
    this.realtimeSub = this.realtimeService.deviceUpdates$.subscribe(dto => {
      const device = this.allDevices.find(d => d.id === String(dto.id));
      if (device) {
        device.state = {
          ...device.state,
          on: dto.stateOn,
          brightness: dto.brightness,
          temperature: dto.temperature,
          sensorValue: dto.sensorValue,
          coverPosition: dto.coverPosition,
        };
      }
      this.activeDevices = this.allDevices.filter(d => d.state.on).length;
    });
  }

  ngOnDestroy(): void {
    this.realtimeSub?.unsubscribe();
    this.realtimeService.disconnect();
  }

  goToRooms(): void {
    this.router.navigate(['/rooms']);
  }

  getRoom(roomId: string): Room | undefined {
    return this.rooms.find(r => r.id === roomId);
  }

  onToggle(device: Device, on: boolean): void {
    this.deviceService.updateState(Number(device.roomId), Number(device.id), { stateOn: on }).subscribe({
      next: () => this.snackBar.open(`${device.name} ${on ? 'turned on' : 'turned off'} ✓`, '', { duration: 2000 }),
      error: () => this.snackBar.open('Failed to update device state.', '', { duration: 2000 })
    });
  }

  onBrightnessChange(device: Device, brightness: number): void {
    this.deviceService.updateState(Number(device.roomId), Number(device.id), { brightness }).subscribe({
      next: () => this.snackBar.open(`${device.name} brightness updated ✓`, '', { duration: 2000 }),
      error: () => this.snackBar.open('Failed to update brightness.', '', { duration: 2000 })
    });
  }

  onTempChange(device: Device, temperature: number): void {
    this.deviceService.updateState(Number(device.roomId), Number(device.id), { temperature }).subscribe({
      next: () => this.snackBar.open(`${device.name} temperature updated ✓`, '', { duration: 2000 }),
      error: () => this.snackBar.open('Failed to update temperature.', '', { duration: 2000 })
    });
  }

  onCoverAction(device: Device, action: string): void {
    if (action === 'open') {
      device.state = { ...device.state, coverPosition: 100 };
      this.deviceService.updateState(Number(device.roomId), Number(device.id), { coverPosition: 100 }).subscribe({
        next: () => this.snackBar.open(`${device.name} opened ✓`, '', { duration: 2000 }),
        error: () => this.snackBar.open('Failed to open shutter.', '', { duration: 2000 })
      });
    } else if (action === 'close') {
      device.state = { ...device.state, coverPosition: 0 };
      this.deviceService.updateState(Number(device.roomId), Number(device.id), { coverPosition: 0 }).subscribe({
        next: () => this.snackBar.open(`${device.name} closed ✓`, '', { duration: 2000 }),
        error: () => this.snackBar.open('Failed to close shutter.', '', { duration: 2000 })
      });
    } else {
      this.snackBar.open(`${device.name} stop command sent`, '', { duration: 2000 });
    }
  }

  activateScene(scene: Scene): void {
    this.snackBar.open(`${scene.name} activated ✓`, '', { duration: 2000 });
  }

  onVacationToggle(active: boolean): void {
    this.snackBar.open(active ? 'Vacation mode enabled ✓' : 'Vacation mode disabled', '', { duration: 2000 });
  }

  relativeTime(date: Date): string {
    const diff = Math.floor((Date.now() - date.getTime()) / 60000);
    if (diff < 1) { return 'just now'; }
    if (diff < 60) { return `${diff} min ago`; }
    return `${Math.floor(diff / 60)}h ago`;
  }

  getDeviceIcon(type: string): string { return DEVICE_ICON[type] ?? 'devices'; }
  getDeviceIconBg(type: string): string { return DEVICE_ICON_BG[type] ?? 'var(--bg)'; }
  getDeviceIconColor(type: string): string { return DEVICE_ICON_COLOR[type] ?? '#94A3B8'; }
}
