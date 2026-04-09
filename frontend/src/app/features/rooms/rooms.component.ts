import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DEVICES } from '../../core/mock-data';
import { Device, Room } from '../../core/models';
import { RoomService, RoomDto } from '../../core/room.service';
import { DeviceCardComponent } from '../../shared/components/device-card/device-card.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { AddDeviceDialogComponent } from './add-device-dialog.component';
import { AddRoomDialogComponent } from './add-room-dialog.component';
import { RenameRoomDialogComponent } from './rename-room-dialog.component';

function toRoom(dto: RoomDto): Room {
  return { id: String(dto.id), name: dto.name, icon: dto.icon };
}

@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatDialogModule, MatSnackBarModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, FormsModule, ReactiveFormsModule,
    DeviceCardComponent, EmptyStateComponent, ConfirmDialogComponent,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Rooms &amp; Devices</h1>
        <p class="subtitle">Manage all your smart devices organized by room.</p>
      </div>

      <!-- Room chips -->
      <div class="room-chips">
        <div
          *ngFor="let room of rooms"
          class="room-chip"
          [class.selected]="selectedRoomId === room.id"
          (click)="selectRoom(room.id)">
          <mat-icon style="font-size:14px;width:14px;height:14px;vertical-align:middle;margin-right:4px;">{{ room.icon }}</mat-icon>
          {{ room.name }}
          <button
            mat-icon-button
            style="width:20px;height:20px;line-height:20px;font-size:14px;margin-left:4px;vertical-align:middle;"
            (click)="$event.stopPropagation(); openRenameRoomDialog(room)"
            title="Rename room">
            <mat-icon style="font-size:14px;width:14px;height:14px;">edit</mat-icon>
          </button>
          <button
            mat-icon-button
            style="width:20px;height:20px;line-height:20px;font-size:14px;vertical-align:middle;"
            (click)="$event.stopPropagation(); onDeleteRoom(room)"
            title="Delete room">
            <mat-icon style="font-size:14px;width:14px;height:14px;">delete</mat-icon>
          </button>
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
  rooms: Room[] = [];
  devices: Device[] = [];
  selectedRoomId = '';

  get filteredDevices() {
    return this.devices.filter(d => d.roomId === this.selectedRoomId);
  }

  constructor(
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private roomService: RoomService,
  ) {}

  ngOnInit() {
    this.loadRooms();
    // Devices still use mock data until device API is implemented
    this.devices = DEVICES.map(d => ({ ...d, state: { ...d.state } }));
  }

  loadRooms() {
    this.loading = true;
    this.roomService.getRooms().subscribe({
      next: (dtos) => {
        this.rooms = dtos.map(toRoom);
        if (this.rooms.length > 0 && !this.selectedRoomId) {
          this.selectedRoomId = this.rooms[0].id;
        }
        this.loading = false;
      },
      error: () => {
        // Fallback: show empty state
        this.loading = false;
      }
    });
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

  /** US-004: Raum mit Name erstellen möglich */
  openAddRoomDialog() {
    const ref = this.dialog.open(AddRoomDialogComponent, { width: '480px' });
    ref.afterClosed().subscribe(result => {
      if (!result) return;
      this.roomService.createRoom({ name: result.name, icon: result.icon }).subscribe({
        next: (dto) => {
          const newRoom = toRoom(dto);
          this.rooms = [...this.rooms, newRoom];
          this.selectedRoomId = newRoom.id;
          this.onSnack(`Room "${result.name}" added ✓`);
        },
        error: () => this.onSnack('Failed to create room. Please try again.')
      });
    });
  }

  /** US-004: Raum umbenennen möglich */
  openRenameRoomDialog(room: Room) {
    const ref = this.dialog.open(RenameRoomDialogComponent, {
      width: '400px',
      data: { currentName: room.name }
    });
    ref.afterClosed().subscribe(newName => {
      if (!newName) return;
      this.roomService.renameRoom(Number(room.id), { name: newName }).subscribe({
        next: (dto) => {
          const updated = toRoom(dto);
          this.rooms = this.rooms.map(r => r.id === room.id ? updated : r);
          this.onSnack(`Room renamed to "${newName}" ✓`);
        },
        error: () => this.onSnack('Failed to rename room. Please try again.')
      });
    });
  }

  /** US-004: Raum löschen möglich (inkl. Hinweis bei vorhandenen Geräten) */
  onDeleteRoom(room: Room) {
    const devicesInRoom = this.devices.filter(d => d.roomId === room.id);
    const hasDevices = devicesInRoom.length > 0;
    const message = hasDevices
      ? `Room "${room.name}" still has ${devicesInRoom.length} device(s). Are you sure you want to delete it anyway?`
      : `Are you sure you want to delete the room "${room.name}"?`;

    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Delete Room', message }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.roomService.deleteRoom(Number(room.id)).subscribe({
        next: () => {
          this.rooms = this.rooms.filter(r => r.id !== room.id);
          if (this.selectedRoomId === room.id) {
            this.selectedRoomId = this.rooms.length > 0 ? this.rooms[0].id : '';
          }
          this.onSnack(`Room "${room.name}" deleted ✓`);
        },
        error: (err) => {
          const msg = err.status === 409
            ? `Room "${room.name}" cannot be deleted because it still has devices.`
            : 'Failed to delete room. Please try again.';
          this.onSnack(msg);
        }
      });
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
    const map: Record<string, string> = {
      switch: 'lightbulb', dimmer: 'lightbulb',
      thermostat: 'thermostat', sensor: 'sensors', cover: 'blinds'
    };
    return map[type] ?? 'devices';
  }
}
