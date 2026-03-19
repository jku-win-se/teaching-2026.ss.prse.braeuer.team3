import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFabButton } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ROOMS, DEVICES } from '../../core/mock-data';
import { Device, Room } from '../../core/models';
import { DeviceCardComponent } from '../../shared/components/device-card/device-card.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { AddDeviceDialogComponent } from './add-device-dialog.component';
import { AddRoomDialogComponent } from './add-room-dialog.component';

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatDialogModule, MatSnackBarModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, FormsModule, ReactiveFormsModule,
    DeviceCardComponent, EmptyStateComponent,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Rooms & Devices</h1>
        <p class="subtitle">Manage all your smart devices organized by room.</p>
      </div>

      <!-- Room chips -->
      <div class="room-chips">
        <div
          *ngFor="let room of rooms"
          class="room-chip"
          [class.selected]="selectedRoomId === room.id"
          (click)="selectRoom(room.id)">
          {{ room.name }}
        </div>
        <div class="room-chip room-chip-add" (click)="openAddRoomDialog()">
          <mat-icon style="font-size:16px;width:16px;height:16px;vertical-align:middle;">add</mat-icon>
          Add Room
        </div>
      </div>

      <!-- Device grid -->
      <div class="device-grid" *ngIf="filteredDevices.length > 0; else emptyDevices">
        <app-device-card
          *ngFor="let device of filteredDevices"
          [device]="device"
          [room]="getRoom(device.roomId)"
          (toggled)="onSnack(device.name + (device.state.on ? ' turned on' : ' turned off') + ' ✓')"
          (sliderChanged)="onSnack(device.name + ' brightness updated ✓')"
          (tempChanged)="onSnack(device.name + ' temperature updated ✓')"
          (coverAction)="onCoverAction(device, $event)"
          (injectValue)="onInjectValue(device)"
          (rename)="onRename(device)"
          (remove)="onRemove(device)">
        </app-device-card>
      </div>

      <ng-template #emptyDevices>
        <app-empty-state
          icon="devices_off"
          title="No devices in this room"
          subtitle="Add your first device to get started."
          actionLabel="+ Add Device"
          (action)="openAddDialog()">
        </app-empty-state>
      </ng-template>
    </div>

    <!-- FAB -->
    <div class="fab-container">
      <button mat-fab color="primary" (click)="openAddDialog()">
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
})
export class RoomsComponent implements OnInit {
  loading = true;
  rooms: Room[] = ROOMS;
  devices: Device[] = [];
  selectedRoomId = 'r1';

  get filteredDevices() {
    return this.devices.filter(d => d.roomId === this.selectedRoomId);
  }

  constructor(private dialog: MatDialog, private snackBar: MatSnackBar) {}

  ngOnInit() {
    setTimeout(() => {
      this.devices = DEVICES.map(d => ({ ...d, state: { ...d.state } }));
      this.loading = false;
    }, 600);
  }

  selectRoom(id: string) { this.selectedRoomId = id; }

  getRoom(roomId: string): Room | undefined {
    return this.rooms.find(r => r.id === roomId);
  }

  onSnack(msg: string) { this.snackBar.open(msg, '', { duration: 2000 }); }

  onCoverAction(device: Device, action: string) {
    if (action === 'open') device.state = { ...device.state, coverPosition: 100 };
    else if (action === 'close') device.state = { ...device.state, coverPosition: 0 };
    this.onSnack(`${device.name} ${action} command sent ✓`);
  }

  onInjectValue(device: Device) {
    const newVal = Math.round(Math.random() * 100);
    device.state = { ...device.state, sensorValue: newVal };
    this.onSnack(`Injected value ${newVal} ${device.state.sensorUnit} into ${device.name} ✓`);
  }

  onRename(device: Device) {
    const newName = prompt(`Rename "${device.name}" to:`, device.name);
    if (newName && newName.trim()) {
      device.name = newName.trim();
      this.onSnack(`Device renamed to "${device.name}" ✓`);
    }
  }

  onRemove(device: Device) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Remove Device', message: `Are you sure you want to remove "${device.name}"? This action cannot be undone.` }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.devices = this.devices.filter(d => d.id !== device.id);
        this.onSnack(`${device.name} removed ✓`);
      }
    });
  }

  openAddRoomDialog() {
    const ref = this.dialog.open(AddRoomDialogComponent, { width: '480px' });
    ref.afterClosed().subscribe(result => {
      if (result) {
        const newRoom: Room = { id: 'r_' + Date.now(), name: result.name, icon: result.icon };
        this.rooms = [...this.rooms, newRoom];
        this.selectedRoomId = newRoom.id;
        this.onSnack(`Room "${result.name}" added ✓`);
      }
    });
  }

  openAddDialog() {
    const ref = this.dialog.open(AddDeviceDialogComponent, {
      width: '480px',
      data: { rooms: this.rooms, defaultRoomId: this.selectedRoomId }
    });
    ref.afterClosed().subscribe(result => {
      if (result) {
        const newDevice: Device = {
          id: 'd_' + Date.now(),
          name: result.name,
          roomId: result.roomId,
          type: result.type,
          icon: this.iconForType(result.type),
          state: { on: false, brightness: 50, temperature: 21, sensorValue: 0, sensorUnit: '°C', coverPosition: 0 },
        };
        this.devices = [...this.devices, newDevice];
        this.selectedRoomId = result.roomId;
        this.onSnack(`${result.name} added ✓`);
      }
    });
  }

  iconForType(type: string): string {
    const map: Record<string, string> = { switch: 'lightbulb', dimmer: 'lightbulb', thermostat: 'thermostat', sensor: 'sensors', cover: 'blinds' };
    return map[type] ?? 'devices';
  }
}
