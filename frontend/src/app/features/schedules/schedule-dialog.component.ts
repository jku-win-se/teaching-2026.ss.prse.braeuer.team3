import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { ScheduleDto, ScheduleRequest, DAYS } from '../../core/models';
import { RoomService } from '../../core/room.service';
import { DeviceService, DeviceDto } from '../../core/device.service';

interface DeviceOption {
  id: number;
  name: string;
  roomName: string;
  type: string;
}

@Component({
  selector: 'app-schedule-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatSlideToggleModule,
    MatProgressSpinnerModule, MatIconModule,
    FormsModule, ReactiveFormsModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.schedule ? 'Edit Schedule' : 'Add Schedule' }}</h2>
    <mat-dialog-content style="min-width:420px;padding-top:8px;">

      <div *ngIf="loadingDevices" style="display:flex;justify-content:center;padding:24px;">
        <mat-spinner diameter="40"></mat-spinner>
      </div>

      <form *ngIf="!loadingDevices" [formGroup]="form" style="display:flex;flex-direction:column;gap:14px;">

        <mat-form-field appearance="outline">
          <mat-label>Schedule name</mat-label>
          <input matInput formControlName="name" placeholder="e.g. Morning Lights">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Device</mat-label>
          <mat-select formControlName="deviceId">
            <mat-option *ngFor="let d of deviceOptions" [value]="d.id">
              {{ d.name }} ({{ d.roomName }})
            </mat-option>
          </mat-select>
          <mat-hint *ngIf="deviceOptions.length === 0">No devices found — add a device first.</mat-hint>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Action</mat-label>
          <mat-select formControlName="action">
            <mat-option value="TURN_ON">{{ selectedDeviceType === 'cover' ? 'Open' : 'Turn On' }}</mat-option>
            <mat-option value="TURN_OFF">{{ selectedDeviceType === 'cover' ? 'Close' : 'Turn Off' }}</mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Time</mat-label>
          <input matInput type="time" formControlName="time">
          <mat-hint>When should this schedule run?</mat-hint>
        </mat-form-field>

        <div>
          <div style="font-size:12px;color:rgba(0,0,0,.6);margin-bottom:6px;">Days of week</div>
          <div style="display:flex;gap:6px;flex-wrap:wrap;">
            <button
              *ngFor="let d of DAYS"
              type="button"
              mat-stroked-button
              [color]="isDaySelected(d.value) ? 'primary' : ''"
              [style.font-weight]="isDaySelected(d.value) ? '600' : '400'"
              (click)="toggleDay(d.value)"
              style="min-width:48px;padding:0 8px;">
              {{ d.label }}
            </button>
          </div>
          <div *ngIf="noDaysSelected" style="font-size:12px;color:#f44336;margin-top:4px;">
            At least one day must be selected.
          </div>
        </div>

        <mat-slide-toggle formControlName="enabled" color="primary">
          Active
        </mat-slide-toggle>

      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button
        mat-flat-button color="primary"
        [disabled]="loadingDevices || !form.valid || noDaysSelected"
        (click)="submit()">
        {{ data.schedule ? 'Save Changes' : 'Add Schedule' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ScheduleDialogComponent implements OnInit {
  form: FormGroup;
  deviceOptions: DeviceOption[] = [];
  loadingDevices = true;

  readonly DAYS = DAYS;

  constructor(
    private fb: FormBuilder,
    private roomService: RoomService,
    private deviceService: DeviceService,
    public dialogRef: MatDialogRef<ScheduleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { schedule: ScheduleDto | null; defaultTime?: string }
  ) {
    const sched = data.schedule;
    const defaultTime = data.defaultTime ?? '07:00';
    const initialTime = sched
      ? `${sched.hour.toString().padStart(2, '0')}:${sched.minute.toString().padStart(2, '0')}`
      : defaultTime;

    let initialAction = 'TURN_ON';
    if (sched?.actionPayload) {
      try {
        const parsed = JSON.parse(sched.actionPayload);
        initialAction = parsed.stateOn === false ? 'TURN_OFF' : 'TURN_ON';
      } catch { /* keep default */ }
    }

    const initialDays = sched?.daysOfWeek?.length
      ? [...sched.daysOfWeek]
      : ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'];

    this.form = this.fb.group({
      name:     [sched?.name ?? '', Validators.required],
      deviceId: [sched?.deviceId ?? null, Validators.required],
      action:   [initialAction, Validators.required],
      time:     [initialTime, Validators.required],
      daysOfWeek: [initialDays],
      enabled:  [sched?.enabled ?? true],
    });
  }

  ngOnInit(): void {
    this.roomService.getRooms().pipe(
      switchMap(rooms => {
        if (!rooms.length) { return of([] as DeviceOption[][]); }
        return forkJoin(
          rooms.map(room =>
            this.deviceService.getDevices(room.id).pipe(
              map((devices: DeviceDto[]) =>
                devices.map(d => ({ id: d.id, name: d.name, roomName: room.name, type: d.type }))
              )
            )
          )
        );
      }),
      map((nested: DeviceOption[][]) => nested.flat())
    ).subscribe({
      next: (devices: DeviceOption[]) => {
        this.deviceOptions = devices.filter(d => d.type === 'switch' || d.type === 'cover');
        this.loadingDevices = false;
      },
      error: () => { this.loadingDevices = false; }
    });
  }

  get selectedDeviceType(): string {
    const device = this.deviceOptions.find(d => d.id === this.form.value.deviceId);
    return device?.type ?? '';
  }

  get noDaysSelected(): boolean {
    return (this.form.value.daysOfWeek as string[]).length === 0;
  }

  isDaySelected(day: string): boolean {
    return (this.form.value.daysOfWeek as string[]).includes(day);
  }

  toggleDay(day: string): void {
    const current: string[] = [...this.form.value.daysOfWeek];
    const idx = current.indexOf(day);
    if (idx >= 0) {
      current.splice(idx, 1);
    } else {
      current.push(day);
    }
    this.form.patchValue({ daysOfWeek: current });
  }

  submit(): void {
    if (!this.form.valid || this.noDaysSelected) { return; }
    const { name, deviceId, action, time, daysOfWeek, enabled } = this.form.value;
    const [hour, minute] = (time as string).split(':').map(Number);
    const isCover = this.selectedDeviceType === 'cover';
    const actionPayload = action === 'TURN_OFF'
      ? (isCover ? '{"stateOn":false,"coverPosition":0}' : '{"stateOn":false}')
      : (isCover ? '{"stateOn":true,"coverPosition":100}' : '{"stateOn":true}');
    const request: ScheduleRequest = { name, deviceId, daysOfWeek, hour, minute, actionPayload, enabled };
    this.dialogRef.close(request);
  }
}
