import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import {
  FormsModule, ReactiveFormsModule, FormBuilder, FormGroup,
  Validators, AbstractControl, ValidationErrors,
} from '@angular/forms';
import { Room, DeviceType } from '../../core/models';

function noWhitespace(control: AbstractControl): ValidationErrors | null {
  return control.value && control.value.trim().length === 0 ? { whitespace: true } : null;
}

interface DeviceTypeOption { value: DeviceType; label: string; icon: string; }

@Component({
  selector: 'app-add-device-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatIconModule, FormsModule, ReactiveFormsModule,
  ],
  template: `
    <h2 mat-dialog-title>Add New Device</h2>
    <mat-dialog-content style="padding-top:8px;min-width:400px;">

      <!-- F3: No rooms warning -->
      <div *ngIf="data.rooms.length === 0"
           style="background:#FFF3E0;border:1px solid #FFCC80;border-radius:8px;padding:12px 16px;margin-bottom:16px;display:flex;align-items:center;gap:8px;">
        <mat-icon style="color:#F57C00;">warning</mat-icon>
        <span style="font-size:14px;color:#E65100;">Please add a room first before adding devices.</span>
      </div>

      <form [formGroup]="form">
        <mat-form-field appearance="outline" style="width:100%;margin-bottom:8px;">
          <mat-label>Device name</mat-label>
          <input matInput formControlName="name" placeholder="e.g. Living Room Speaker">
          <mat-hint>Give your device a friendly name (max 50 characters)</mat-hint>
          <mat-error *ngIf="form.get('name')?.hasError('required')">Name is required</mat-error>
          <mat-error *ngIf="form.get('name')?.hasError('whitespace')">Name cannot be only spaces</mat-error>
          <mat-error *ngIf="form.get('name')?.hasError('maxlength')">Name must not exceed 50 characters</mat-error>
        </mat-form-field>

        <div style="margin-bottom:16px;">
          <div style="font-size:14px;color:#424242;margin-bottom:8px;font-weight:500;">Device Type</div>
          <div style="display:grid;grid-template-columns:repeat(5,1fr);gap:8px;">
            <div
              *ngFor="let opt of deviceTypes"
              (click)="selectType(opt.value)"
              style="padding:12px 4px;border-radius:8px;border:2px solid;text-align:center;cursor:pointer;transition:all 0.2s;"
              [style.borderColor]="selectedType === opt.value ? '#00897B' : '#e0e0e0'"
              [style.background]="selectedType === opt.value ? 'rgba(0,137,123,0.06)' : 'white'">
              <mat-icon style="display:block;margin:0 auto 4px;" [style.color]="selectedType === opt.value ? '#00897B' : '#9e9e9e'">{{ opt.icon }}</mat-icon>
              <div style="font-size:11px;" [style.color]="selectedType === opt.value ? '#00897B' : '#616161'">{{ opt.label }}</div>
            </div>
          </div>
          <!-- F2: type selection hint shown after first submit attempt -->
          <div *ngIf="submitted && !selectedType"
               style="font-size:12px;color:#f44336;margin-top:6px;">
            Please select a device type
          </div>
        </div>

        <mat-form-field appearance="outline" style="width:100%;">
          <mat-label>Room</mat-label>
          <mat-select formControlName="roomId">
            <mat-option *ngFor="let room of data.rooms" [value]="room.id">{{ room.name }}</mat-option>
          </mat-select>
          <mat-hint>Choose which room this device is in</mat-hint>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="data.rooms.length === 0" (click)="submit()">Add Device</button>
    </mat-dialog-actions>
  `,
})
export class AddDeviceDialogComponent {
  selectedType: DeviceType | null = null;
  submitted = false;
  form: FormGroup;

  deviceTypes: DeviceTypeOption[] = [
    { value: 'switch', label: 'Switch', icon: 'toggle_on' },
    { value: 'dimmer', label: 'Dimmer', icon: 'lightbulb' },
    { value: 'thermostat', label: 'Thermostat', icon: 'thermostat' },
    { value: 'sensor', label: 'Sensor', icon: 'sensors' },
    { value: 'cover', label: 'Shutter', icon: 'blinds' },
  ];

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<AddDeviceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { rooms: Room[]; defaultRoomId: string }
  ) {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(50), noWhitespace]],
      roomId: [this.data.defaultRoomId, Validators.required],
    });
  }

  selectType(type: DeviceType) { this.selectedType = type; }

  submit() {
    this.submitted = true;
    this.form.markAllAsTouched();
    if (this.form.valid && this.selectedType) {
      const trimmedName = (this.form.value.name as string).trim();
      this.dialogRef.close({ name: trimmedName, roomId: this.form.value.roomId, type: this.selectedType });
    }
  }
}
