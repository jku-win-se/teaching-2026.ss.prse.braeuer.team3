import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { DEVICES, ROOMS } from '../../core/mock-data';
import { TriggerType, Device } from '../../core/models';

const SENSOR_DEVICES = DEVICES.filter(d => d.type === 'sensor');
const CONTROLLABLE_DEVICES = DEVICES.filter(d => d.type !== 'sensor');
const ALL_DEVICES = DEVICES;

function roomName(roomId: string): string {
  return ROOMS.find(r => r.id === roomId)?.name ?? '';
}

@Component({
  selector: 'app-new-rule-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatStepperModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule,
    FormsModule, ReactiveFormsModule, MatCardModule, MatButtonToggleModule,
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
    .inline-row .op-field { width: 100px; flex-shrink: 0; }
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
    .step-actions.right { justify-content: flex-end; }
  `],
  template: `
    <h2 mat-dialog-title>Create New Rule</h2>
    <mat-dialog-content style="min-width:500px; padding-top:8px;">
      <mat-stepper [linear]="true" #stepper orientation="horizontal">

        <!-- Step 1: Name -->
        <mat-step [stepControl]="nameForm">
          <ng-template matStepLabel>Name</ng-template>
          <div class="step-content">
            <form [formGroup]="nameForm">
              <mat-form-field appearance="outline" style="width:100%;">
                <mat-label>Rule name</mat-label>
                <input matInput formControlName="name" placeholder="e.g. Night Mode">
                <mat-hint>A short, descriptive name for this automation</mat-hint>
                <mat-error>Name is required</mat-error>
              </mat-form-field>
            </form>
          </div>
          <div class="step-actions">
            <button mat-button mat-dialog-close>Cancel</button>
            <button mat-flat-button color="primary" matStepperNext [disabled]="!nameForm.valid">Next</button>
          </div>
        </mat-step>

        <!-- Step 2: Trigger -->
        <mat-step>
          <ng-template matStepLabel>Trigger</ng-template>
          <div class="step-content">
            <p style="font-size:13px;color:#616161;margin:0 0 14px;">What should trigger this rule?</p>

            <!-- Trigger type selection -->
            <div class="trigger-type-buttons">
              <div class="trigger-btn" [class.selected]="selectedTrigger === 'time'" (click)="setTrigger('time')">
                <mat-icon>schedule</mat-icon>
                <span>Time</span>
              </div>
              <div class="trigger-btn" [class.selected]="selectedTrigger === 'threshold'" (click)="setTrigger('threshold')">
                <mat-icon>show_chart</mat-icon>
                <span>Sensor Threshold</span>
              </div>
              <div class="trigger-btn" [class.selected]="selectedTrigger === 'event'" (click)="setTrigger('event')">
                <mat-icon>bolt</mat-icon>
                <span>Device Event</span>
              </div>
            </div>

            <!-- Time trigger details -->
            <div *ngIf="selectedTrigger === 'time'" style="margin-top:20px;">
              <mat-form-field appearance="outline" style="width:100%; margin-bottom:16px;">
                <mat-label>Time</mat-label>
                <input matInput type="time" [(ngModel)]="triggerTime">
              </mat-form-field>
              <p style="font-size:13px;font-weight:500;color:#424242;margin:0 0 8px;">Repeat on</p>
              <div class="day-toggles">
                <div *ngFor="let day of days; let i = index"
                     class="day-btn" [class.active]="selectedDays.includes(i)"
                     (click)="toggleDay(i)">{{ day }}</div>
              </div>
              <p class="day-hint" *ngIf="selectedDays.length === 0" style="color:#e53935;">Select at least one day</p>
              <p class="day-hint" *ngIf="selectedDays.length > 0">Repeats every {{ dayRepeatLabel }}</p>
            </div>

            <!-- Threshold trigger details -->
            <div *ngIf="selectedTrigger === 'threshold'" style="margin-top:20px; display:flex; flex-direction:column; gap:12px;">
              <mat-form-field appearance="outline" style="width:100%;">
                <mat-label>Sensor</mat-label>
                <mat-select [(ngModel)]="thresholdSensorId" (ngModelChange)="thresholdValue = ''">
                  <mat-option *ngFor="let s of sensorDevices" [value]="s.id">
                    {{ s.name }} ({{ roomName(s.roomId) }})
                  </mat-option>
                </mat-select>
                <mat-hint>Which sensor to monitor</mat-hint>
              </mat-form-field>
              <div class="inline-row" *ngIf="thresholdSensorId">
                <mat-form-field appearance="outline" class="op-field">
                  <mat-label>Operator</mat-label>
                  <mat-select [(ngModel)]="thresholdOperator">
                    <mat-option value=">">&gt; (above)</mat-option>
                    <mat-option value="<">&lt; (below)</mat-option>
                    <mat-option value=">=">&ge; (at least)</mat-option>
                    <mat-option value="<=">&le; (at most)</mat-option>
                    <mat-option value="==">= (equals)</mat-option>
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline" class="val-field">
                  <mat-label>Value</mat-label>
                  <input matInput type="number" [(ngModel)]="thresholdValue" placeholder="e.g. 28">
                  <span matTextSuffix *ngIf="sensorUnit">{{ sensorUnit }}</span>
                </mat-form-field>
              </div>
              <p *ngIf="thresholdSensorId && thresholdValue" style="font-size:13px;color:#00897B;margin:0;">
                Triggers when {{ selectedSensor?.name }} {{ thresholdOperator }} {{ thresholdValue }} {{ sensorUnit }}
              </p>
            </div>

            <!-- Event trigger details -->
            <div *ngIf="selectedTrigger === 'event'" style="margin-top:20px; display:flex; flex-direction:column; gap:12px;">
              <mat-form-field appearance="outline" style="width:100%;">
                <mat-label>Device</mat-label>
                <mat-select [(ngModel)]="eventDeviceId" (ngModelChange)="onEventDeviceChange()">
                  <mat-option *ngFor="let d of allDevices" [value]="d.id">
                    {{ d.name }} ({{ roomName(d.roomId) }})
                  </mat-option>
                </mat-select>
                <mat-hint>Which device should trigger the rule</mat-hint>
              </mat-form-field>
              <mat-form-field appearance="outline" style="width:100%;" *ngIf="eventDeviceId">
                <mat-label>Event</mat-label>
                <mat-select [(ngModel)]="eventType">
                  <mat-option *ngFor="let opt of eventOptionsList" [value]="opt.value">
                    {{ opt.label }}
                  </mat-option>
                </mat-select>
                <mat-hint>What event from this device triggers the rule</mat-hint>
              </mat-form-field>
              <p *ngIf="eventDeviceId && eventType" style="font-size:13px;color:#00897B;margin:0;">
                Triggers when {{ eventDevice?.name }}: {{ getEventLabel() }}
              </p>
            </div>
          </div>
          <div class="step-actions">
            <button mat-button matStepperPrevious>Back</button>
            <button mat-flat-button color="primary" matStepperNext [disabled]="!triggerValid">Next</button>
          </div>
        </mat-step>

        <!-- Step 3: Action -->
        <mat-step [stepControl]="actionForm">
          <ng-template matStepLabel>Action</ng-template>
          <div class="step-content">
            <p style="font-size:13px;color:#616161;margin:0 0 14px;">What should happen when the rule triggers?</p>
            <form [formGroup]="actionForm" style="display:flex; flex-direction:column; gap:12px;">
              <mat-form-field appearance="outline" style="width:100%;">
                <mat-label>Target device</mat-label>
                <mat-select formControlName="device" (selectionChange)="onTargetDeviceChange()">
                  <mat-option *ngFor="let d of controllableDevices" [value]="d.id">
                    {{ d.name }} ({{ roomName(d.roomId) }})
                  </mat-option>
                </mat-select>
                <mat-hint>Which device should be controlled</mat-hint>
              </mat-form-field>
              <mat-form-field appearance="outline" style="width:100%;" *ngIf="actionForm.value.device">
                <mat-label>Action</mat-label>
                <mat-select formControlName="action">
                  <mat-option *ngFor="let opt of actionOptionsList" [value]="opt.value">
                    {{ opt.label }}
                  </mat-option>
                </mat-select>
              </mat-form-field>
            </form>
          </div>
          <div class="step-actions">
            <button mat-button matStepperPrevious>Back</button>
            <button mat-flat-button color="primary" matStepperNext [disabled]="!actionForm.valid">Next</button>
          </div>
        </mat-step>

        <!-- Step 4: Review -->
        <mat-step>
          <ng-template matStepLabel>Review</ng-template>
          <div class="step-content">
            <p style="font-size:13px;color:#616161;margin:0 0 12px;">Review your new rule before saving.</p>
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
          </div>
          <div class="step-actions">
            <button mat-button matStepperPrevious>Back</button>
            <button mat-flat-button color="primary" (click)="save()">Save Rule</button>
          </div>
        </mat-step>

      </mat-stepper>
    </mat-dialog-content>
  `,
})
export class NewRuleDialogComponent {
  nameForm: any;
  actionForm: any;

  selectedTrigger: TriggerType | null = null;

  // Time
  triggerTime = '07:00';
  selectedDays: number[] = [0, 1, 2, 3, 4];
  readonly days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  // Threshold
  thresholdSensorId = '';
  thresholdOperator = '>';
  thresholdValue = '';

  // Event
  eventDeviceId = '';
  eventType = '';
  eventOptionsList: { value: string; label: string }[] = [];
  actionOptionsList: { value: string; label: string }[] = [];

  readonly sensorDevices = SENSOR_DEVICES;
  readonly controllableDevices = CONTROLLABLE_DEVICES;
  readonly allDevices = ALL_DEVICES;
  readonly roomName = roomName;

  constructor(private fb: FormBuilder, private dialogRef: MatDialogRef<NewRuleDialogComponent>) {
    this.nameForm = this.fb.group({ name: ['', Validators.required] });
    this.actionForm = this.fb.group({
      device: ['', Validators.required],
      action: ['', Validators.required],
    });
  }

  setTrigger(type: TriggerType) {
    this.selectedTrigger = type;
    this.thresholdSensorId = '';
    this.thresholdValue = '';
    this.eventDeviceId = '';
    this.eventType = '';
    this.eventOptionsList = [];
  }

  toggleDay(day: number) {
    if (this.selectedDays.includes(day)) {
      this.selectedDays = this.selectedDays.filter(d => d !== day);
    } else {
      this.selectedDays = [...this.selectedDays, day].sort((a, b) => a - b);
    }
  }

  get triggerValid(): boolean {
    if (!this.selectedTrigger) return false;
    if (this.selectedTrigger === 'time') return !!this.triggerTime && this.selectedDays.length > 0;
    if (this.selectedTrigger === 'threshold') return !!this.thresholdSensorId && !!this.thresholdValue;
    if (this.selectedTrigger === 'event') return !!this.eventDeviceId && !!this.eventType;
    return false;
  }

  get selectedSensor(): Device | undefined {
    return SENSOR_DEVICES.find(d => d.id === this.thresholdSensorId);
  }

  get sensorUnit(): string {
    return this.selectedSensor?.state?.sensorUnit ?? '';
  }

  get eventDevice(): Device | undefined {
    return ALL_DEVICES.find(d => d.id === this.eventDeviceId);
  }

  private computeEventOptions(deviceId: string): { value: string; label: string }[] {
    const device = ALL_DEVICES.find(d => d.id === deviceId);
    const type = device?.type;
    if (!type) return [];
    if (type === 'sensor') {
      const unit = device?.state?.sensorUnit;
      if (unit === 'motion') return [{ value: 'motion', label: 'Motion detected' }];
      return [{ value: 'threshold', label: 'Reading exceeds threshold' }];
    }
    if (type === 'cover') return [
      { value: 'opened', label: 'Opened' },
      { value: 'closed', label: 'Closed' },
    ];
    return [
      { value: 'turned-on', label: 'Turned on' },
      { value: 'turned-off', label: 'Turned off' },
    ];
  }

  get dayRepeatLabel(): string {
    if (this.selectedDays.length === 7) return 'day';
    const isWeekdays = this.selectedDays.length === 5 && !this.selectedDays.includes(5) && !this.selectedDays.includes(6);
    if (isWeekdays) return 'weekday';
    const isWeekends = this.selectedDays.length === 2 && this.selectedDays.includes(5) && this.selectedDays.includes(6);
    if (isWeekends) return 'weekend';
    return this.selectedDays.map(d => this.days[d]).join(', ');
  }

  getEventLabel(): string {
    return this.eventOptionsList.find(e => e.value === this.eventType)?.label ?? this.eventType;
  }

  onEventDeviceChange() {
    this.eventType = '';
    this.eventOptionsList = this.computeEventOptions(this.eventDeviceId);
  }

  private computeActionOptions(deviceId: string): { value: string; label: string }[] {
    const device = CONTROLLABLE_DEVICES.find(d => d.id === deviceId);
    if (!device) return [];
    if (device.type === 'cover') return [
      { value: 'Open', label: 'Open' },
      { value: 'Close', label: 'Close' },
    ];
    if (device.type === 'dimmer') return [
      { value: 'Turn on', label: 'Turn on (100%)' },
      { value: 'Turn off', label: 'Turn off' },
      { value: 'Set to 20%', label: 'Set brightness to 20%' },
      { value: 'Set to 50%', label: 'Set brightness to 50%' },
      { value: 'Set to 80%', label: 'Set brightness to 80%' },
    ];
    if (device.type === 'thermostat') return [
      { value: 'Turn on', label: 'Turn on' },
      { value: 'Turn off', label: 'Turn off' },
      { value: 'Set to 18°C', label: 'Set temperature to 18°C' },
      { value: 'Set to 20°C', label: 'Set temperature to 20°C' },
      { value: 'Set to 22°C', label: 'Set temperature to 22°C' },
      { value: 'Set to 24°C', label: 'Set temperature to 24°C' },
    ];
    return [
      { value: 'Turn on', label: 'Turn on' },
      { value: 'Turn off', label: 'Turn off' },
    ];
  }

  onTargetDeviceChange() {
    this.actionForm.patchValue({ action: '' });
    this.actionOptionsList = this.computeActionOptions(this.actionForm.value.device);
  }

  get triggerIcon(): string {
    if (this.selectedTrigger === 'time') return 'schedule';
    if (this.selectedTrigger === 'threshold') return 'show_chart';
    return 'bolt';
  }

  get triggerLabel(): string {
    if (this.selectedTrigger === 'time') {
      const isDaily = this.selectedDays.length === 7;
      const isWeekdays = this.selectedDays.length === 5 && !this.selectedDays.includes(5) && !this.selectedDays.includes(6);
      const isWeekends = this.selectedDays.length === 2 && this.selectedDays.includes(5) && this.selectedDays.includes(6);
      const dayStr = isDaily ? 'Every day' : isWeekdays ? 'Weekdays' : isWeekends ? 'Weekends' : this.dayRepeatLabel;
      return `${dayStr} at ${this.triggerTime}`;
    }
    if (this.selectedTrigger === 'threshold') {
      return `${this.selectedSensor?.name ?? 'Sensor'} ${this.thresholdOperator} ${this.thresholdValue} ${this.sensorUnit}`;
    }
    if (this.selectedTrigger === 'event') {
      const evt = this.eventOptionsList.find(e => e.value === this.eventType);
      return `${this.eventDevice?.name ?? 'Device'}: ${evt?.label ?? this.eventType}`;
    }
    return '';
  }

  get actionLabel(): string {
    const deviceId = this.actionForm.value.device;
    const device = CONTROLLABLE_DEVICES.find(d => d.id === deviceId);
    const action = this.actionForm.value.action;
    if (!device || !action) return '—';
    return `${action} → ${device.name} (${roomName(device.roomId)})`;
  }

  save() {
    this.dialogRef.close({
      name: this.nameForm.value.name,
      triggerType: this.selectedTrigger,
      triggerLabel: this.triggerLabel,
      action: this.actionLabel,
    });
  }
}
