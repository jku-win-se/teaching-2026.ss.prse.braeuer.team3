import { Component, Inject, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatStepperModule, MatStepper } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { forkJoin } from 'rxjs';
import { RuleDto, RuleRequest, TriggerType, DAYS } from '../../core/models';
import { RuleService } from '../../core/rule.service';
import { RoomService } from '../../core/room.service';
import { DeviceService, DeviceDto } from '../../core/device.service';

interface RoomOption { id: number; name: string; }
interface DeviceOption { id: number; name: string; type: string; }

@Component({
  selector: 'app-new-rule-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatStepperModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule,
    FormsModule, ReactiveFormsModule, MatCardModule, MatProgressSpinnerModule,
  ],
  styles: [`
    .trigger-type-buttons { display: flex; gap: 12px; }
    .trigger-btn {
      flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px;
      padding: 16px 8px; border: 2px solid #e0e0e0; border-radius: 10px;
      cursor: pointer; transition: all .2s; font-size: 13px; color: #424242;
    }
    .trigger-btn:hover { border-color: #00897B; background: #f0faf9; }
    .trigger-btn.selected { border-color: #00897B; background: #e0f2f1; color: #00897B; font-weight: 500; }
    .trigger-btn mat-icon { font-size: 28px; width: 28px; height: 28px; }
    .day-toggles { display: flex; gap: 6px; flex-wrap: wrap; }
    .day-btn {
      min-width: 40px; padding: 4px 0; border: 1.5px solid #bdbdbd; border-radius: 20px;
      font-size: 12px; font-weight: 500; text-align: center; cursor: pointer;
      color: #616161; background: #fff; transition: all .15s;
    }
    .day-btn.active { border-color: #00897B; background: #00897B; color: #fff; }
    .day-hint { font-size: 12px; color: #9e9e9e; margin-top: 4px; }
    .inline-row { display: flex; gap: 12px; align-items: flex-start; }
    .inline-row .op-field { width: 120px; flex-shrink: 0; }
    .inline-row .val-field { flex: 1; }
    .review-card {
      background: #f9f9f9; border: 1px solid #e0e0e0; border-radius: 8px;
      padding: 16px; margin-top: 4px;
    }
    .review-label { font-size: 12px; color: #757575; margin: 0 0 4px; }
    .review-row { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #212121; margin: 6px 0; }
    .review-row mat-icon { font-size: 18px; width: 18px; height: 18px; color: #00897B; }
    .step-content { padding: 12px 0 4px; }
    .step-actions { display: flex; justify-content: space-between; margin-top: 16px; }
    .conflict-warning {
      margin-top: 14px; padding: 12px 14px;
      border: 1.5px solid #ffcc02; border-radius: 8px; background: #fff8e1;
    }
    .conflict-item {
      display: flex; align-items: center; gap: 6px;
      padding: 4px 0; border-top: 1px solid #ffe082;
    }
  `],
  template: `
    <h2 mat-dialog-title>{{ editMode ? 'Edit Rule' : 'Create New Rule' }}</h2>
    <mat-dialog-content style="min-width:520px; padding-top:8px;">

      <div *ngIf="loadingDevices" style="display:flex;justify-content:center;padding:32px;">
        <mat-spinner diameter="40"></mat-spinner>
      </div>

      <div *ngIf="!loadingDevices && rooms.length === 0"
           style="display:flex;flex-direction:column;align-items:center;gap:12px;padding:32px 16px;color:#757575;">
        <mat-icon style="font-size:40px;width:40px;height:40px;">meeting_room</mat-icon>
        <p style="margin:0;font-size:14px;text-align:center;">
          No rooms or devices found.<br>Add a room and at least one device first.
        </p>
        <button mat-flat-button mat-dialog-close>Close</button>
      </div>

      <mat-stepper [linear]="true" #stepper orientation="horizontal" *ngIf="!loadingDevices && rooms.length > 0">

        <!-- Step 1: Name -->
        <mat-step [stepControl]="nameForm">
          <ng-template matStepLabel>Name</ng-template>
          <div class="step-content">
            <form [formGroup]="nameForm">
              <mat-form-field appearance="outline" style="width:100%;">
                <mat-label>Rule name</mat-label>
                <input matInput formControlName="name" placeholder="e.g. Night Mode" data-testid="rule-name-input">
                <mat-hint>A short, descriptive name for this automation</mat-hint>
                <mat-error>Name is required</mat-error>
              </mat-form-field>
            </form>
          </div>
          <div class="step-actions">
            <button mat-button mat-dialog-close>Cancel</button>
            <button mat-flat-button color="primary" matStepperNext [disabled]="!nameForm.valid" data-testid="rule-name-next">Next</button>
          </div>
        </mat-step>

        <!-- Step 2: Trigger -->
        <mat-step>
          <ng-template matStepLabel>Trigger</ng-template>
          <div class="step-content">
            <p style="font-size:13px;color:#616161;margin:0 0 14px;">What should trigger this rule?</p>

            <div class="trigger-type-buttons">
              <div class="trigger-btn" [class.selected]="selectedTrigger === 'TIME'" (click)="setTrigger('TIME')" data-testid="trigger-time">
                <mat-icon>schedule</mat-icon><span>Time</span>
              </div>
              <div class="trigger-btn" [class.selected]="selectedTrigger === 'THRESHOLD'" (click)="setTrigger('THRESHOLD')" data-testid="trigger-threshold">
                <mat-icon>show_chart</mat-icon><span>Sensor Threshold</span>
              </div>
              <div class="trigger-btn" [class.selected]="selectedTrigger === 'EVENT'" (click)="setTrigger('EVENT')" data-testid="trigger-event">
                <mat-icon>bolt</mat-icon><span>Device Event</span>
              </div>
            </div>

            <!-- TIME trigger -->
            <div *ngIf="selectedTrigger === 'TIME'" style="margin-top:20px;">
              <mat-form-field appearance="outline" style="width:100%;margin-bottom:16px;">
                <mat-label>Time</mat-label>
                <input matInput type="time" [(ngModel)]="triggerTime" data-testid="trigger-time-input">
              </mat-form-field>
              <p style="font-size:13px;font-weight:500;color:#424242;margin:0 0 8px;">Repeat on</p>
              <div class="day-toggles">
                <div *ngFor="let day of days; let i = index"
                     class="day-btn" [class.active]="selectedDays.includes(i)"
                     (click)="toggleDay(i)" [attr.data-testid]="'day-' + day.label">{{ day.label }}</div>
              </div>
              <p class="day-hint" *ngIf="selectedDays.length === 0" style="color:#e53935;">Select at least one day</p>
            </div>

            <!-- THRESHOLD trigger -->
            <div *ngIf="selectedTrigger === 'THRESHOLD'" style="margin-top:20px;display:flex;flex-direction:column;gap:12px;">
              <mat-form-field appearance="outline" style="width:100%;">
                <mat-label>Room</mat-label>
                <mat-select [(ngModel)]="thresholdRoomId" (ngModelChange)="onThresholdRoomChange()" data-testid="threshold-room-select">
                  <mat-option *ngFor="let r of rooms" [value]="r.id">{{ r.name }}</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline" style="width:100%;" *ngIf="thresholdRoomId">
                <mat-label>Sensor Device</mat-label>
                <mat-select [(ngModel)]="thresholdDeviceId" data-testid="threshold-device-select">
                  <mat-option *ngFor="let d of thresholdDevices" [value]="d.id">{{ d.name }}</mat-option>
                </mat-select>
                <mat-hint>Which sensor to monitor</mat-hint>
              </mat-form-field>
              <div class="inline-row" *ngIf="thresholdDeviceId">
                <mat-form-field appearance="outline" class="op-field">
                  <mat-label>Operator</mat-label>
                  <mat-select [(ngModel)]="thresholdOperator" data-testid="threshold-operator-select">
                    <mat-option value="GT">&gt; (above)</mat-option>
                    <mat-option value="LT">&lt; (below)</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline" class="val-field">
                  <mat-label>Value</mat-label>
                  <input matInput type="number" [(ngModel)]="thresholdValue" placeholder="e.g. 28" data-testid="threshold-value-input">
                </mat-form-field>
              </div>
            </div>

            <!-- EVENT trigger -->
            <div *ngIf="selectedTrigger === 'EVENT'" style="margin-top:20px;display:flex;flex-direction:column;gap:12px;">
              <mat-form-field appearance="outline" style="width:100%;">
                <mat-label>Room</mat-label>
                <mat-select [(ngModel)]="eventRoomId" (ngModelChange)="onEventRoomChange()" data-testid="event-room-select">
                  <mat-option *ngFor="let r of rooms" [value]="r.id">{{ r.name }}</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline" style="width:100%;" *ngIf="eventRoomId">
                <mat-label>Device</mat-label>
                <mat-select [(ngModel)]="eventDeviceId" data-testid="event-device-select">
                  <mat-option *ngFor="let d of eventDevices" [value]="d.id">{{ d.name }}</mat-option>
                </mat-select>
                <mat-hint>Which device's state change triggers the rule</mat-hint>
              </mat-form-field>
            </div>
          </div>
          <div class="step-actions">
            <button mat-button matStepperPrevious>Back</button>
            <button mat-flat-button color="primary" matStepperNext [disabled]="!triggerValid" data-testid="trigger-next">Next</button>
          </div>
        </mat-step>

        <!-- Step 3: Action -->
        <mat-step [stepControl]="actionForm">
          <ng-template matStepLabel>Action</ng-template>
          <div class="step-content">
            <p style="font-size:13px;color:#616161;margin:0 0 14px;">What should happen when the rule triggers?</p>
            <form [formGroup]="actionForm" style="display:flex;flex-direction:column;gap:12px;">
              <mat-form-field appearance="outline" style="width:100%;">
                <mat-label>Room</mat-label>
                <mat-select formControlName="room" (selectionChange)="onActionRoomChange()" data-testid="action-room-select">
                  <mat-option *ngFor="let r of rooms" [value]="r.id">{{ r.name }}</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline" style="width:100%;" *ngIf="actionForm.value.room">
                <mat-label>Target device</mat-label>
                <mat-select formControlName="device" (selectionChange)="onActionDeviceChange()" data-testid="action-device-select">
                  <mat-option *ngFor="let d of actionDevices" [value]="d.id">{{ d.name }}</mat-option>
                </mat-select>
              </mat-form-field>
              <mat-form-field appearance="outline" style="width:100%;" *ngIf="actionForm.value.device">
                <mat-label>Action</mat-label>
                <mat-select formControlName="action" data-testid="action-value-select">
                  <mat-option *ngFor="let opt of actionOptionsList" [value]="opt.value">{{ opt.label }}</mat-option>
                </mat-select>
              </mat-form-field>
            </form>
          </div>
          <div class="step-actions">
            <button mat-button matStepperPrevious>Back</button>
            <button mat-flat-button color="primary" [disabled]="!actionForm.valid"
                    (click)="goToReview()" data-testid="action-next">Next</button>
          </div>
        </mat-step>

        <!-- Step 4: Review -->
        <mat-step>
          <ng-template matStepLabel>Review</ng-template>
          <div class="step-content">
            <p style="font-size:13px;color:#616161;margin:0 0 12px;">Review your rule before saving.</p>
            <div class="review-card">
              <p class="review-label">Rule name</p>
              <p style="font-size:16px;font-weight:600;color:#212121;margin:0 0 12px;">{{ nameForm.value.name }}</p>
              <p class="review-label">Trigger</p>
              <div class="review-row">
                <mat-icon>{{ triggerIcon }}</mat-icon>
                <span>{{ triggerLabel }}</span>
              </div>
              <p class="review-label" style="margin-top:10px;">Action</p>
              <div class="review-row">
                <mat-icon>play_arrow</mat-icon>
                <span>{{ actionLabel }}</span>
              </div>
            </div>

            <!-- Conflict warning (US-014) -->
            <div *ngIf="checkingConflicts" style="display:flex;align-items:center;gap:8px;margin-top:14px;color:#757575;font-size:13px;">
              <mat-spinner diameter="18"></mat-spinner>
              <span>Checking for rule conflicts…</span>
            </div>

            <div *ngIf="conflictCheckError"
                 style="margin-top:14px;padding:10px 14px;border:1.5px solid #ef9a9a;border-radius:8px;background:#ffebee;font-size:13px;color:#c62828;"
                 data-testid="conflict-check-error">
              <mat-icon style="font-size:16px;width:16px;height:16px;vertical-align:middle;">error_outline</mat-icon>
              Conflict check failed — {{ conflictCheckErrorDetail }}
            </div>

            <div *ngIf="!checkingConflicts && conflictingRules.length > 0"
                 class="conflict-warning"
                 data-testid="conflict-warning-panel">
              <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;">
                <mat-icon style="color:#e65100;font-size:20px;width:20px;height:20px;">warning</mat-icon>
                <strong style="color:#e65100;font-size:13px;">
                  Conflict detected — {{ conflictingRules.length }} existing rule{{ conflictingRules.length > 1 ? 's' : '' }} control{{ conflictingRules.length === 1 ? 's' : '' }} this device with the opposite action
                </strong>
              </div>
              <div *ngFor="let r of conflictingRules" class="conflict-item" [attr.data-testid]="'conflict-item-' + r.id">
                <mat-icon style="font-size:16px;width:16px;height:16px;color:#9e9e9e;">rule</mat-icon>
                <span style="font-size:13px;color:#424242;">
                  <strong>{{ r.name }}</strong> — {{ buildConflictSummary(r) }}
                </span>
              </div>
              <p style="font-size:12px;color:#757575;margin:8px 0 0;">
                You can still save this rule. The conflicting rules may run at different times or under different conditions.
              </p>
            </div>
          </div>
          <div class="step-actions">
            <button mat-button matStepperPrevious>Back</button>
            <button mat-flat-button color="primary" (click)="save()" [disabled]="saving || checkingConflicts" data-testid="rule-save-btn">
              {{ saving ? 'Saving…' : (editMode ? 'Update Rule' : 'Save Rule') }}
            </button>
          </div>
        </mat-step>

      </mat-stepper>
    </mat-dialog-content>
  `,
})
export class NewRuleDialogComponent implements OnInit {
  nameForm: FormGroup;
  actionForm: FormGroup;

  @ViewChild('stepper') stepper!: MatStepper;

  editMode = false;
  saving = false;
  loadingDevices = true;
  checkingConflicts = false;
  conflictingRules: RuleDto[] = [];
  conflictCheckError = false;
  conflictCheckErrorDetail = '';

  selectedTrigger: TriggerType | null = null;

  // TIME
  triggerTime = '07:00';
  selectedDays: number[] = [0, 1, 2, 3, 4];
  readonly days = DAYS;

  // THRESHOLD
  thresholdRoomId: number | null = null;
  thresholdDevices: DeviceOption[] = [];
  thresholdDeviceId: number | null = null;
  thresholdOperator: 'GT' | 'LT' = 'GT';
  thresholdValue = '';

  // EVENT
  eventRoomId: number | null = null;
  eventDevices: DeviceOption[] = [];
  eventDeviceId: number | null = null;

  // Action
  actionDevices: DeviceOption[] = [];
  actionOptionsList: { value: string; label: string }[] = [];

  rooms: RoomOption[] = [];
  private allDevicesByRoom = new Map<number, DeviceDto[]>();

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<NewRuleDialogComponent>,
    private ruleService: RuleService,
    private roomService: RoomService,
    private deviceService: DeviceService,
    @Inject(MAT_DIALOG_DATA) public data: RuleDto | null,
  ) {
    this.editMode = !!data;
    this.nameForm = this.fb.group({ name: [data?.name ?? '', Validators.required] });
    this.actionForm = this.fb.group({
      room: ['', Validators.required],
      device: ['', Validators.required],
      action: ['', Validators.required],
    });
  }

  ngOnInit() {
    this.roomService.getRooms().subscribe(rooms => {
      this.rooms = rooms.map(r => ({ id: r.id, name: r.name }));
      if (rooms.length === 0) {
        this.loadingDevices = false;
        return;
      }
      const deviceRequests = rooms.map(r => this.deviceService.getDevices(r.id));
      forkJoin(deviceRequests).subscribe(allDevices => {
        rooms.forEach((r, i) => this.allDevicesByRoom.set(r.id, allDevices[i]));
        this.loadingDevices = false;
        if (this.editMode && this.data) {
          this.prefillFromEdit(this.data);
        }
      });
    });
  }

  private prefillFromEdit(rule: RuleDto) {
    this.selectedTrigger = rule.triggerType;

    if (rule.triggerType === 'TIME') {
      const h = String(rule.triggerHour ?? 0).padStart(2, '0');
      const m = String(rule.triggerMinute ?? 0).padStart(2, '0');
      this.triggerTime = `${h}:${m}`;
      const dayValues = (rule.triggerDaysOfWeek ?? '').split(',');
      this.selectedDays = this.days
        .map((d, i) => dayValues.includes(d.value) ? i : -1)
        .filter(i => i >= 0);
    } else if (rule.triggerType === 'THRESHOLD' && rule.triggerDeviceId) {
      const roomId = this.findRoomForDevice(rule.triggerDeviceId);
      if (roomId) {
        this.thresholdRoomId = roomId;
        this.thresholdDevices = this.devicesForRoom(roomId, 'sensor');
        this.thresholdDeviceId = rule.triggerDeviceId;
        this.thresholdOperator = rule.triggerOperator ?? 'GT';
        this.thresholdValue = String(rule.triggerThresholdValue ?? '');
      }
    } else if (rule.triggerType === 'EVENT' && rule.triggerDeviceId) {
      const roomId = this.findRoomForDevice(rule.triggerDeviceId);
      if (roomId) {
        this.eventRoomId = roomId;
        this.eventDevices = this.devicesForRoom(roomId, null);
        this.eventDeviceId = rule.triggerDeviceId;
      }
    }

    const actionRoomId = this.findRoomForDevice(rule.actionDeviceId);
    if (actionRoomId) {
      // patchValue first so that actionForm.value.room is set before computeActionOptions reads it
      this.actionForm.patchValue({
        room: actionRoomId,
        device: rule.actionDeviceId,
        action: rule.actionValue,
      });
      this.actionDevices = this.devicesForRoom(actionRoomId, null, true);
      this.actionOptionsList = this.computeActionOptions(rule.actionDeviceId);
    }
  }

  private findRoomForDevice(deviceId: number): number | null {
    for (const [roomId, devices] of this.allDevicesByRoom) {
      if (devices.some(d => d.id === deviceId)) return roomId;
    }
    return null;
  }

  private devicesForRoom(roomId: number, typeFilter: string | null, excludeSensors = false): DeviceOption[] {
    const devices = this.allDevicesByRoom.get(roomId) ?? [];
    return devices
      .filter(d => typeFilter ? d.type === typeFilter : true)
      .filter(d => excludeSensors ? d.type !== 'sensor' : true)
      .map(d => ({ id: d.id, name: d.name, type: d.type }));
  }

  setTrigger(type: TriggerType) {
    this.selectedTrigger = type;
    this.thresholdRoomId = null; this.thresholdDeviceId = null; this.thresholdValue = '';
    this.eventRoomId = null; this.eventDeviceId = null;
  }

  toggleDay(day: number) {
    if (this.selectedDays.includes(day)) {
      this.selectedDays = this.selectedDays.filter(d => d !== day);
    } else {
      this.selectedDays = [...this.selectedDays, day].sort((a, b) => a - b);
    }
  }

  onThresholdRoomChange() {
    this.thresholdDeviceId = null;
    this.thresholdDevices = this.thresholdRoomId
      ? this.devicesForRoom(this.thresholdRoomId, 'sensor')
      : [];
  }

  onEventRoomChange() {
    this.eventDeviceId = null;
    this.eventDevices = this.eventRoomId
      ? this.devicesForRoom(this.eventRoomId, null)
      : [];
  }

  onActionRoomChange() {
    this.actionForm.patchValue({ device: '', action: '' });
    this.actionDevices = this.actionForm.value.room
      ? this.devicesForRoom(this.actionForm.value.room, null, true)
      : [];
  }

  onActionDeviceChange() {
    this.actionForm.patchValue({ action: '' });
    this.actionOptionsList = this.computeActionOptions(this.actionForm.value.device);
  }

  private computeActionOptions(deviceId: number): { value: string; label: string }[] {
    const devices = this.allDevicesByRoom.get(this.actionForm.value.room) ?? [];
    const device = devices.find(d => d.id === deviceId);
    if (!device) return [];
    if (device.type === 'cover') return [
      { value: 'open', label: 'Open' },
      { value: 'close', label: 'Close' },
    ];
    return [
      { value: 'true', label: 'Turn on' },
      { value: 'false', label: 'Turn off' },
    ];
  }

  get triggerValid(): boolean {
    if (!this.selectedTrigger) return false;
    if (this.selectedTrigger === 'TIME') return !!this.triggerTime && this.selectedDays.length > 0;
    if (this.selectedTrigger === 'THRESHOLD') return !!this.thresholdDeviceId && !!this.thresholdValue;
    if (this.selectedTrigger === 'EVENT') return !!this.eventDeviceId;
    return false;
  }

  get triggerIcon(): string {
    if (this.selectedTrigger === 'TIME') return 'schedule';
    if (this.selectedTrigger === 'THRESHOLD') return 'show_chart';
    return 'bolt';
  }

  get triggerLabel(): string {
    if (this.selectedTrigger === 'TIME') {
      const dayLabels = this.selectedDays.map(i => this.days[i].label).join(', ');
      return `${dayLabels} at ${this.triggerTime}`;
    }
    if (this.selectedTrigger === 'THRESHOLD') {
      const device = this.thresholdDevices.find(d => d.id === this.thresholdDeviceId);
      const op = this.thresholdOperator === 'GT' ? '>' : '<';
      return `${device?.name ?? 'Sensor'} ${op} ${this.thresholdValue}`;
    }
    if (this.selectedTrigger === 'EVENT') {
      const device = this.eventDevices.find(d => d.id === this.eventDeviceId);
      return `${device?.name ?? 'Device'} state changes`;
    }
    return '';
  }

  get actionLabel(): string {
    const device = this.actionDevices.find(d => d.id === this.actionForm.value.device);
    const action = this.actionOptionsList.find(o => o.value === this.actionForm.value.action);
    if (!device || !action) return '—';
    return `${action.label} → ${device.name}`;
  }

  /**
   * Advances the stepper to the Review step and immediately triggers the conflict check.
   * Using an explicit stepper.next() call is more reliable than relying on
   * the selectionChange event together with the matStepperNext directive.
   */
  goToReview() {
    this.conflictingRules = [];
    this.conflictCheckError = false;
    this.stepper.next();
    this.runConflictCheck();
  }

  /** Runs the backend conflict check for the currently selected action device and value. */
  private runConflictCheck() {
    const deviceId = this.actionForm.value.device as number | null;
    const actionValue = this.actionForm.value.action as string | null;
    if (!deviceId || !actionValue) {
      return;
    }

    this.checkingConflicts = true;
    this.conflictingRules = [];
    this.conflictCheckError = false;
    const excludeId = this.editMode && this.data ? this.data.id : undefined;
    this.ruleService.checkConflicts(deviceId, actionValue, excludeId).subscribe({
      next: conflicts => {
        this.conflictingRules = conflicts;
        this.checkingConflicts = false;
      },
      error: err => {
        this.conflictCheckError = true;
        this.conflictCheckErrorDetail = `HTTP ${err?.status ?? '?'}: ${err?.error?.message ?? err?.message ?? 'unknown'}`;
        this.checkingConflicts = false;
      },
    });
  }

  /** @deprecated Kept for selectionChange binding compatibility — delegates to runConflictCheck. */
  onStepChange(selectedIndex: number) {
    if (selectedIndex === 3) {
      this.runConflictCheck();
    }
  }

  /** Builds a short human-readable summary of a conflicting rule's action for display. */
  buildConflictSummary(rule: RuleDto): string {
    const actionText = rule.actionValue === 'true' ? 'turns ON'
      : rule.actionValue === 'false' ? 'turns OFF'
      : rule.actionValue === 'open' ? 'opens'
      : rule.actionValue === 'close' ? 'closes'
      : rule.actionValue;
    return `${actionText} ${rule.actionDeviceName}`;
  }

  save() {
    const [hours, minutes] = this.triggerTime.split(':').map(Number);
    const daysOfWeek = this.selectedDays.map(i => this.days[i].value).join(',');

    const req: RuleRequest = {
      name: this.nameForm.value.name,
      enabled: true,
      triggerType: this.selectedTrigger!,
      actionDeviceId: this.actionForm.value.device,
      actionValue: this.actionForm.value.action,
    };

    if (this.selectedTrigger === 'TIME') {
      req.triggerHour = hours;
      req.triggerMinute = minutes;
      req.triggerDaysOfWeek = daysOfWeek;
    } else if (this.selectedTrigger === 'THRESHOLD') {
      req.triggerDeviceId = this.thresholdDeviceId!;
      req.triggerOperator = this.thresholdOperator;
      req.triggerThresholdValue = Number(this.thresholdValue);
    } else {
      req.triggerDeviceId = this.eventDeviceId!;
    }

    this.saving = true;
    const call = this.editMode && this.data
      ? this.ruleService.updateRule(this.data.id, req)
      : this.ruleService.createRule(req);

    call.subscribe({
      next: result => { this.saving = false; this.dialogRef.close(result); },
      error: () => { this.saving = false; },
    });
  }
}
