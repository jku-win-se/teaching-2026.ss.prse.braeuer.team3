import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Room, DeviceType } from '../../core/models';

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
      <form [formGroup]="form">
        <mat-form-field appearance="outline" style="width:100%;margin-bottom:8px;">
          <mat-label>Device name</mat-label>
          <input matInput formControlName="name" placeholder="e.g. Living Room Speaker">
          <mat-hint>Give your device a friendly name</mat-hint>
          <mat-error *ngIf="form.get('name')?.hasError('required')">Name is required</mat-error>
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
      <button mat-flat-button color="primary" [disabled]="!form.valid || !selectedType" (click)="submit()">Add Device</button>
    </mat-dialog-actions>
  `,
})
export class AddDeviceDialogComponent {
  selectedType: DeviceType | null = null;
  form: any;

  deviceTypes: DeviceTypeOption[] = [
    { value: 'switch', label: 'Switch', icon: 'toggle_on' },
    { value: 'dimmer', label: 'Dimmer', icon: 'lightbulb' },
    { value: 'thermostat', label: 'Thermostat', icon: 'thermostat' },
    { value: 'sensor', label: 'Sensor', icon: 'sensors' },
    { value: 'cover', label: 'Cover', icon: 'blinds' },
  ];

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<AddDeviceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { rooms: Room[]; defaultRoomId: string }
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      roomId: [this.data.defaultRoomId, Validators.required],
    });
  }

  selectType(type: DeviceType) { this.selectedType = type; }

  submit() {
    if (this.form.valid && this.selectedType) {
      this.dialogRef.close({ ...this.form.value, type: this.selectedType });
    }
  }
}
