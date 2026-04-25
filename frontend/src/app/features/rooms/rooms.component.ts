import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
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
import { Subscription } from 'rxjs';
import { Device, DeviceType, Room } from '../../core/models';
import { RoomService, RoomDto } from '../../core/room.service';
import { DeviceService, DeviceDto } from '../../core/device.service';
import { toRoom, dtoToDevice, DEVICE_ICON } from '../../core/device-utils';
import { RealtimeService } from '../../core/realtime.service';
import { DeviceCardComponent } from '../../shared/components/device-card/device-card.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { ConnectionStatusComponent } from '../../shared/components/connection-status/connection-status.component';
import { AddDeviceDialogComponent } from './add-device-dialog.component';
import { AddRoomDialogComponent } from './add-room-dialog.component';
import { InjectValueDialogComponent } from './inject-value-dialog.component';
import { RenameDeviceDialogComponent } from './rename-device-dialog.component';
import { RenameRoomDialogComponent } from './rename-room-dialog.component';


@Component({
  selector: 'app-rooms',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatDialogModule, MatSnackBarModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, FormsModule, ReactiveFormsModule,
    DeviceCardComponent, EmptyStateComponent, ConnectionStatusComponent, AsyncPipe,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <app-connection-status [state]="(realtimeService.state$ | async) ?? 'disconnected'"></app-connection-status>
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
            *ngIf="selectedRoomId === room.id"
            mat-icon-button
            style="width:24px;height:24px;padding:0;margin-left:4px;position:relative;"
            (click)="$event.stopPropagation(); openRenameRoomDialog(room)"
            title="Rename room">
            <mat-icon style="font-size:15px;width:15px;height:15px;position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);margin:0;">edit</mat-icon>
          </button>
          <button
            *ngIf="selectedRoomId === room.id"
            mat-icon-button
            style="width:24px;height:24px;padding:0;position:relative;"
            (click)="$event.stopPropagation(); onDeleteRoom(room)"
            title="Delete room">
            <mat-icon style="font-size:15px;width:15px;height:15px;position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);margin:0;">delete</mat-icon>
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
          (toggled)="onToggle(device, $event)"
          (sliderChanged)="onBrightnessChange(device, $event)"
          (tempChanged)="onTempChange(device, $event)"
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
export class RoomsComponent implements OnInit, OnDestroy {
  loading = true;
  rooms: Room[] = [];
  devices: Device[] = [];
  selectedRoomId = '';

  private sseSubscription: Subscription | null = null;

  get filteredDevices() {
    return this.devices.filter(d => d.roomId === this.selectedRoomId);
  }

  constructor(
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private roomService: RoomService,
    private deviceService: DeviceService,
    readonly realtimeService: RealtimeService,
  ) {}

  ngOnInit() {
    this.loadRooms();
    this.realtimeService.connect();
    this.sseSubscription = this.realtimeService.deviceUpdates$.subscribe(dto => {
      const device = this.devices.find(d => d.id === String(dto.id));
      if (device) {
        device.state = {
          on: dto.stateOn,
          brightness: dto.brightness,
          temperature: dto.temperature,
          sensorValue: dto.sensorValue,
          sensorUnit: device.state.sensorUnit,
          coverPosition: dto.coverPosition,
        };
      }
    });
  }

  ngOnDestroy() {
    this.sseSubscription?.unsubscribe();
    this.realtimeService.disconnect();
  }

  loadRooms() {
    this.loading = true;
    this.roomService.getRooms().subscribe({
      next: (dtos) => {
        this.rooms = dtos.map(toRoom);
        if (this.rooms.length > 0 && !this.selectedRoomId) {
          this.selectedRoomId = this.rooms[0].id;
          this.loadDevices(this.selectedRoomId);
        }
        this.loading = false;
      },
      error: () => {
        // Fallback: show empty state
        this.loading = false;
      }
    });
  }

  selectRoom(id: string) {
    this.selectedRoomId = id;
    this.loadDevices(id);
  }

  loadDevices(roomId: string) {
    this.deviceService.getDevices(Number(roomId)).subscribe({
      next: (dtos) => {
        this.devices = dtos.map((dto: DeviceDto) => dtoToDevice(dto, roomId));
      },
      error: () => { this.devices = []; }
    });
  }

  getRoom(roomId: string): Room | undefined {
    return this.rooms.find(r => r.id === roomId);
  }

  onSnack(msg: string) { this.snackBar.open(msg, '', { duration: 2000 }); }

  onToggle(device: Device, on: boolean) {
    this.deviceService.updateState(Number(device.roomId), Number(device.id), { stateOn: on }).subscribe({
      next: () => this.onSnack(`${device.name} ${on ? 'turned on' : 'turned off'} ✓`),
      error: () => this.onSnack('Failed to update device state.')
    });
  }

  onBrightnessChange(device: Device, brightness: number) {
    this.deviceService.updateState(Number(device.roomId), Number(device.id), { brightness }).subscribe({
      next: () => this.onSnack(`${device.name} brightness updated ✓`),
      error: () => this.onSnack('Failed to update brightness.')
    });
  }

  onTempChange(device: Device, temperature: number) {
    this.deviceService.updateState(Number(device.roomId), Number(device.id), { temperature }).subscribe({
      next: () => this.onSnack(`${device.name} temperature updated ✓`),
      error: () => this.onSnack('Failed to update temperature.')
    });
  }

  onCoverAction(device: Device, action: string) {
    if (action === 'open') {
      device.state = { ...device.state, coverPosition: 100 };
      this.deviceService.updateState(Number(device.roomId), Number(device.id), { coverPosition: 100 }).subscribe({
        next: () => this.onSnack(`${device.name} opened ✓`),
        error: () => this.onSnack('Failed to open cover.')
      });
    } else if (action === 'close') {
      device.state = { ...device.state, coverPosition: 0 };
      this.deviceService.updateState(Number(device.roomId), Number(device.id), { coverPosition: 0 }).subscribe({
        next: () => this.onSnack(`${device.name} closed ✓`),
        error: () => this.onSnack('Failed to close cover.')
      });
    } else {
      this.onSnack(`${device.name} stop command sent`);
    }
  }

  onInjectValue(device: Device) {
    const ref = this.dialog.open(InjectValueDialogComponent, { width: '360px' });
    ref.afterClosed().subscribe(value => {
      if (value === null || value === undefined) { return; }
      device.state = { ...device.state, sensorValue: value };
      this.deviceService.updateState(Number(device.roomId), Number(device.id), { sensorValue: value }).subscribe({
        next: () => this.onSnack(`Sensor value set to ${value} ✓`),
        error: () => this.onSnack('Failed to update sensor value.')
      });
    });
  }

  onRename(device: Device) {
    const ref = this.dialog.open(RenameDeviceDialogComponent, {
      width: '400px',
      data: { currentName: device.name }
    });
    ref.afterClosed().subscribe(newName => {
      if (!newName) { return; }
      this.deviceService.renameDevice(Number(device.roomId), Number(device.id), newName).subscribe({
        next: (dto) => {
          device.name = dto.name;
          this.onSnack(`Device renamed to "${dto.name}" ✓`);
        },
        error: (err: { status: number }) => {
          const msg = err.status === 409
            ? `A device named "${newName}" already exists in this room.`
            : 'Failed to rename device. Please try again.';
          this.onSnack(msg);
        }
      });
    });
  }

  onRemove(device: Device) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Remove Device', message: `Are you sure you want to remove "${device.name}"? This action cannot be undone.` }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) { return; }
      this.deviceService.removeDevice(Number(device.roomId), Number(device.id)).subscribe({
        next: () => {
          this.devices = this.devices.filter(d => d.id !== device.id);
          this.onSnack(`${device.name} removed ✓`);
        },
        error: () => this.onSnack('Failed to remove device. Please try again.')
      });
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
        error: (err: { status: number }) => {
          const msg = err.status === 409
            ? 'A room with this name already exists.'
            : 'Failed to create room. Please try again.';
          this.onSnack(msg);
        }
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
        error: (err: { status: number }) => {
          const msg = err.status === 409
            ? 'A room with this name already exists.'
            : 'Failed to rename room. Please try again.';
          this.onSnack(msg);
        }
      });
    });
  }

  /** US-004: Raum löschen möglich (inkl. Hinweis bei vorhandenen Geräten) */
  onDeleteRoom(room: Room) {
    const devicesInRoom = this.devices.filter(d => d.roomId === room.id);
    const hasDevices = devicesInRoom.length > 0;
    const message = hasDevices
      ? `Deleting room "${room.name}" will permanently remove it and all ${devicesInRoom.length} device(s) inside. This cannot be undone.`
      : `Are you sure you want to delete the room "${room.name}"? This cannot be undone.`;

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
      if (!result) { return; }
      this.deviceService.addDevice(Number(result.roomId), {
        name: result.name,
        type: result.type as DeviceType,
      }).subscribe({
        next: () => {
          this.onSnack(`${result.name} added ✓`);
          this.selectRoom(result.roomId);
        },
        error: (err: { status: number }) => {
          const msg = err.status === 409
            ? `A device named "${result.name}" already exists in this room.`
            : 'Failed to add device. Please try again.';
          this.onSnack(msg);
        }
      });
    });
  }

  iconForType(type: string): string {
    return DEVICE_ICON[type] ?? 'devices';
  }
}
