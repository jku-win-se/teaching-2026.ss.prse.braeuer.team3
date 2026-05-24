import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin } from 'rxjs';

import { RoomService, RoomDto } from '../../core/room.service';
import { DeviceService, DeviceDto } from '../../core/device.service';
import { SimulationService } from '../../core/simulation.service';
import { SimulationEvent, DeviceStartCondition } from '../../core/models';

/** Flat view model merging room + device for the start-conditions list. */
interface DeviceRow {
  device: DeviceDto;
  roomName: string;
  stateOn: boolean;
}

/** A SimulationEvent annotated with redundancy information. */
interface TimelineEvent extends SimulationEvent {
  /** True if the action had no effect because the device was already in the target state. */
  redundant: boolean;
}

/** Events grouped by hour for display. */
interface HourGroup {
  hour: number;
  events: TimelineEvent[];
}

const DAYS_OF_WEEK = [
  { value: 'MONDAY',    label: 'Monday' },
  { value: 'TUESDAY',   label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY',  label: 'Thursday' },
  { value: 'FRIDAY',    label: 'Friday' },
  { value: 'SATURDAY',  label: 'Saturday' },
  { value: 'SUNDAY',    label: 'Sunday' },
];

@Component({
  selector: 'app-simulation',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatIconModule, MatButtonModule, MatSelectModule,
    MatSlideToggleModule, MatProgressBarModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatDividerModule, MatTooltipModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>

    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Day Simulation</h1>
        <p class="subtitle">
          Test your automations by simulating a full 24-hour day in time-lapse.
          The live system is never affected.
        </p>
      </div>

      <div class="sim-layout">

        <!-- ── LEFT: Configuration ─────────────────────────────────── -->
        <mat-card class="sim-config-card">
          <mat-card-header>
            <mat-icon mat-card-avatar style="color:var(--primary)">tune</mat-icon>
            <mat-card-title>Simulation Setup</mat-card-title>
            <mat-card-subtitle>Set starting conditions &amp; select day</mat-card-subtitle>
          </mat-card-header>

          <mat-card-content>

            <!-- Day selector -->
            <div class="config-section">
              <div class="section-label">
                <mat-icon>calendar_today</mat-icon>
                Day to Simulate
              </div>
              <mat-select [(ngModel)]="selectedDay" class="day-select">
                <mat-option *ngFor="let d of days" [value]="d.value">{{ d.label }}</mat-option>
              </mat-select>
            </div>

            <mat-divider style="margin: 16px 0;"></mat-divider>

            <!-- Device start conditions -->
            <div class="config-section">
              <div class="section-label">
                <mat-icon>devices</mat-icon>
                Starting Device States
              </div>

              <div *ngIf="deviceRows.length === 0" class="no-devices">
                <mat-icon>info</mat-icon>
                No devices found. Add devices in Rooms &amp; Devices first.
              </div>

              <div class="device-conditions">
                <div *ngFor="let row of deviceRows" class="device-row">
                  <div class="device-info">
                    <mat-icon class="device-type-icon">{{ typeIcon(row.device.type) }}</mat-icon>
                    <div class="device-names">
                      <span class="device-name">{{ row.device.name }}</span>
                      <span class="room-name">{{ row.roomName }}</span>
                    </div>
                  </div>
                  <mat-slide-toggle
                    [(ngModel)]="row.stateOn"
                    color="primary"
                    [matTooltip]="row.stateOn ? 'Starts ON' : 'Starts OFF'">
                  </mat-slide-toggle>
                </div>
              </div>
            </div>
          </mat-card-content>

          <mat-card-actions style="padding: 8px 16px 16px;">
            <button
              mat-raised-button
              color="primary"
              style="width:100%;"
              [disabled]="running || !selectedDay"
              (click)="runSimulation()">
              <mat-icon>play_circle</mat-icon>
              <span *ngIf="!running">Run Simulation</span>
              <span *ngIf="running">Simulating…</span>
            </button>
          </mat-card-actions>
        </mat-card>

        <!-- ── RIGHT: Results ──────────────────────────────────────── -->
        <mat-card class="sim-results-card">
          <mat-card-header>
            <mat-icon mat-card-avatar style="color:var(--primary)">timeline</mat-icon>
            <mat-card-title>Simulation Timeline</mat-card-title>
            <mat-card-subtitle *ngIf="!hasRun">Run the simulation to see results</mat-card-subtitle>
            <mat-card-subtitle *ngIf="hasRun && hourGroups.length === 0">
              No rules fire on {{ selectedDayLabel }} — all quiet.
            </mat-card-subtitle>
            <mat-card-subtitle *ngIf="hasRun && hourGroups.length > 0">
              {{ totalEvents }} automation event{{ totalEvents === 1 ? '' : 's' }} on {{ selectedDayLabel
              }}<span *ngIf="redundantEvents > 0" class="redundant-badge">
                · {{ redundantEvents }} redundant
              </span>
            </mat-card-subtitle>
          </mat-card-header>

          <mat-card-content class="timeline-content">

            <!-- Spinner while running -->
            <div *ngIf="running" class="running-state">
              <mat-spinner diameter="48"></mat-spinner>
              <p>Simulating 24 hours…</p>
            </div>

            <!-- Empty pre-run state -->
            <div *ngIf="!running && !hasRun" class="empty-results">
              <mat-icon>play_circle_outline</mat-icon>
              <p>Configure starting conditions on the left, then click <strong>Run Simulation</strong>.</p>
            </div>

            <!-- Empty result after run -->
            <div *ngIf="!running && hasRun && hourGroups.length === 0" class="empty-results">
              <mat-icon>check_circle_outline</mat-icon>
              <p>No automation rules are scheduled for this day. Try a different day or add TIME-based rules.</p>
            </div>

            <!-- Event timeline -->
            <div *ngIf="!running && hasRun && hourGroups.length > 0" class="timeline">
              <div *ngFor="let group of hourGroups" class="hour-group">
                <div class="hour-label">
                  <span class="hour-badge">{{ group.hour | number:'2.0-0' }}:00</span>
                </div>
                <div class="hour-events">
                  <div
                    *ngFor="let event of group.events"
                    class="timeline-event"
                    [class.timeline-event--redundant]="event.redundant"
                    [matTooltip]="event.redundant ? 'No-op: device was already in this state' : ''"
                    matTooltipPosition="left">
                    <div class="event-time">
                      {{ event.hour | number:'2.0-0' }}:{{ event.minute | number:'2.0-0' }}
                    </div>
                    <div class="event-dot" [style.background]="actionColor(event.actionValue)"></div>
                    <div class="event-body">
                      <div class="event-device">
                        <mat-icon class="event-icon" [style.color]="actionColor(event.actionValue)">
                          {{ actionIcon(event.actionValue) }}
                        </mat-icon>
                        <strong>{{ event.deviceName }}</strong>
                        <span class="event-room">· {{ event.roomName }}</span>
                      </div>
                      <div class="event-action">{{ actionLabel(event.actionValue) }}</div>
                      <div *ngIf="event.redundant" class="event-redundant-hint">
                        <mat-icon style="font-size:11px;height:11px;width:11px;vertical-align:middle;">info_outline</mat-icon>
                        Already in this state — rule fired but had no effect
                      </div>
                      <div class="event-rule">
                        <mat-icon style="font-size:12px;height:12px;width:12px;vertical-align:middle;">rule</mat-icon>
                        {{ event.ruleName }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

          </mat-card-content>
        </mat-card>

      </div>
    </div>
  `,
  styles: [`
    .sim-layout {
      display: grid;
      grid-template-columns: 360px 1fr;
      gap: 24px;
      align-items: start;
    }
    @media (max-width: 900px) {
      .sim-layout { grid-template-columns: 1fr; }
    }

    .sim-config-card { position: sticky; top: 80px; }

    .config-section { margin-bottom: 8px; }
    .section-label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      font-weight: 600;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 10px;
    }
    .section-label mat-icon { font-size: 16px; height: 16px; width: 16px; }

    .day-select { width: 100%; }

    .device-conditions { display: flex; flex-direction: column; gap: 2px; }
    .device-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #F1F5F9;
    }
    .device-row:last-child { border-bottom: none; }
    .device-info { display: flex; align-items: center; gap: 10px; }
    .device-type-icon { font-size: 20px; height: 20px; width: 20px; color: var(--text-muted); }
    .device-names { display: flex; flex-direction: column; }
    .device-name { font-size: 13px; font-weight: 500; color: var(--text); }
    .room-name { font-size: 11px; color: var(--text-muted); }

    .no-devices {
      display: flex; align-items: center; gap: 8px;
      color: var(--text-muted); font-size: 13px; padding: 12px 0;
    }

    /* Results */
    .sim-results-card { min-height: 400px; }
    .timeline-content { padding-top: 0; }

    .running-state, .empty-results {
      display: flex; flex-direction: column; align-items: center;
      justify-content: center; gap: 16px;
      padding: 60px 20px;
      color: var(--text-muted);
      text-align: center;
    }
    .empty-results mat-icon { font-size: 48px; height: 48px; width: 48px; opacity: 0.4; }

    /* Timeline */
    .timeline { display: flex; flex-direction: column; gap: 0; }
    .hour-group { display: flex; gap: 16px; margin-bottom: 4px; }
    .hour-label {
      width: 52px;
      flex-shrink: 0;
      padding-top: 12px;
      text-align: right;
    }
    .hour-badge {
      font-size: 11px;
      font-weight: 600;
      color: var(--text-muted);
      background: #F1F5F9;
      padding: 2px 6px;
      border-radius: 6px;
      font-variant-numeric: tabular-nums;
    }
    .hour-events { flex: 1; border-left: 2px solid #E2E8F0; padding-left: 16px; padding-bottom: 8px; }

    .timeline-event {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 10px 0;
      position: relative;
    }
    .event-time {
      font-size: 11px;
      font-weight: 600;
      color: var(--text-muted);
      font-variant-numeric: tabular-nums;
      min-width: 36px;
      padding-top: 2px;
    }
    .event-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      flex-shrink: 0;
      margin-top: 5px;
      position: absolute;
      left: -21px;
      border: 2px solid white;
    }
    .event-body { flex: 1; }
    .event-device {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      color: var(--text);
    }
    .event-icon { font-size: 16px; height: 16px; width: 16px; }
    .event-room { color: var(--text-muted); font-size: 12px; }
    .event-action {
      font-size: 12px;
      color: var(--text-muted);
      margin-top: 2px;
    }
    .event-rule {
      font-size: 11px;
      color: #94A3B8;
      margin-top: 2px;
    }

    /* Redundant (no-op) events */
    .event-redundant-hint {
      font-size: 11px;
      color: #F59E0B;
      margin-top: 3px;
      display: flex;
      align-items: center;
      gap: 3px;
    }
    .redundant-badge {
      color: #F59E0B;
      font-weight: 500;
    }
  `],
})
export class SimulationComponent implements OnInit {

  loading = true;
  running = false;
  hasRun = false;

  days = DAYS_OF_WEEK;
  selectedDay = 'MONDAY';

  deviceRows: DeviceRow[] = [];
  hourGroups: HourGroup[] = [];
  totalEvents = 0;
  redundantEvents = 0;

  get selectedDayLabel(): string {
    return this.days.find(d => d.value === this.selectedDay)?.label ?? this.selectedDay;
  }

  constructor(
    private roomService: RoomService,
    private deviceService: DeviceService,
    private simulationService: SimulationService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.loadDevices();
  }

  /** Load all rooms and their devices to build the start-conditions list. */
  private loadDevices(): void {
    this.roomService.getRooms().subscribe({
      next: (rooms: RoomDto[]) => {
        if (rooms.length === 0) {
          this.loading = false;
          return;
        }
        const calls = rooms.map(r => this.deviceService.getDevices(r.id));
        forkJoin(calls).subscribe({
          next: (deviceLists: DeviceDto[][]) => {
            this.deviceRows = [];
            rooms.forEach((room, i) => {
              deviceLists[i].forEach((device: DeviceDto) => {
                this.deviceRows.push({ device, roomName: room.name, stateOn: device.stateOn });
              });
            });
            this.loading = false;
          },
          error: () => {
            this.snackBar.open('Failed to load devices.', 'Dismiss', { duration: 4000 });
            this.loading = false;
          },
        });
      },
      error: () => {
        this.snackBar.open('Failed to load rooms.', 'Dismiss', { duration: 4000 });
        this.loading = false;
      },
    });
  }

  /** Build start conditions from current row states and send to backend. */
  runSimulation(): void {
    if (!this.selectedDay || this.running) {
      return;
    }
    this.running = true;
    this.hasRun = false;

    const startConditions: DeviceStartCondition[] = this.deviceRows.map(row => ({
      deviceId: row.device.id,
      stateOn: row.stateOn,
      brightness: row.device.brightness,
      temperature: row.device.temperature,
      sensorValue: row.device.sensorValue,
      coverPosition: row.device.coverPosition,
    }));

    this.simulationService.runSimulation({ dayOfWeek: this.selectedDay, startConditions }).subscribe({
      next: response => {
        this.buildTimeline(response.events);
        this.running = false;
        this.hasRun = true;
      },
      error: () => {
        this.snackBar.open('Simulation failed. Please try again.', 'Dismiss', { duration: 5000 });
        this.running = false;
      },
    });
  }

  /** Group flat event list by hour for display, annotating redundant (no-op) events. */
  private buildTimeline(events: SimulationEvent[]): void {
    // Initialise simulated device state from the chosen start conditions.
    const deviceStateOn = new Map<number, boolean>();
    this.deviceRows.forEach(row => deviceStateOn.set(row.device.id, row.stateOn));

    // Sort chronologically so state advances in the correct order.
    const sorted = [...events].sort(
      (a, b) => a.hour * 60 + a.minute - (b.hour * 60 + b.minute),
    );

    // Annotate each event and advance the tracked state.
    let redundantCount = 0;
    const annotated: TimelineEvent[] = sorted.map(e => {
      let redundant = false;
      const av = e.actionValue?.toLowerCase();
      if (av === 'true' || av === 'false') {
        const targetOn = av === 'true';
        const currentOn = deviceStateOn.get(e.deviceId) ?? false;
        redundant = targetOn === currentOn;
        deviceStateOn.set(e.deviceId, targetOn);
      }
      if (redundant) {
        redundantCount++;
      }
      return { ...e, redundant };
    });

    this.totalEvents = annotated.length;
    this.redundantEvents = redundantCount;

    const map = new Map<number, TimelineEvent[]>();
    annotated.forEach(e => {
      if (!map.has(e.hour)) {
        map.set(e.hour, []);
      }
      map.get(e.hour)!.push(e);
    });
    this.hourGroups = Array.from(map.entries())
      .sort(([a], [b]) => a - b)
      .map(([hour, evts]) => ({ hour, events: evts }));
  }

  // ── Display helpers ──────────────────────────────────────────────────────────

  typeIcon(type: string): string {
    const icons: Record<string, string> = {
      switch: 'toggle_on', dimmer: 'light_mode', thermostat: 'thermostat',
      sensor: 'sensors', cover: 'blinds',
    };
    return icons[type] ?? 'device_unknown';
  }

  actionIcon(actionValue: string): string {
    switch (actionValue?.toLowerCase()) {
      case 'true':  return 'power';
      case 'false': return 'power_off';
      case 'open':  return 'expand';
      case 'close': return 'compress';
      default:      return 'settings';
    }
  }

  actionLabel(actionValue: string): string {
    switch (actionValue?.toLowerCase()) {
      case 'true':  return 'Switched ON';
      case 'false': return 'Switched OFF';
      case 'open':  return 'Cover opened';
      case 'close': return 'Cover closed';
      default:      return actionValue;
    }
  }

  actionColor(actionValue: string): string {
    switch (actionValue?.toLowerCase()) {
      case 'true':
      case 'open':  return '#10B981';
      case 'false':
      case 'close': return '#EF4444';
      default:      return '#6B7280';
    }
  }
}
