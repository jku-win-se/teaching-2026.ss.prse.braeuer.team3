import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { SceneDto, SceneEntryDto } from '../../core/models';
import { RoomService } from '../../core/room.service';
import { DeviceService } from '../../core/device.service';

const SCENE_ICONS = [
  'wb_sunny', 'bedtime', 'movie', 'flight_takeoff', 'celebration',
  'weekend', 'kitchen', 'bed', 'auto_awesome', 'mode_night',
  'bolt', 'shield', 'eco', 'favorite', 'star',
  'home', 'local_florist', 'music_note', 'restaurant', 'sports_esports',
  'beach_access', 'directions_car', 'fitness_center', 'work', 'school',
];

interface DeviceOption {
  id: number;
  name: string;
  type: string;
}

interface ActionOption {
  label: string;
  value: string;
}

/**
 * One row in the device-action list.
 * `actions` is a stable reference so *ngFor never re-creates mat-option elements
 * on every change-detection cycle (which would cause a cascading freeze).
 */
interface EntryPair {
  deviceId: number | null;
  actionValue: string;
  /** Pre-computed and stable — only replaced when the selected device changes. */
  actions: ActionOption[];
}

/** Returns the allowed action values for a given device type. */
function actionsForType(type: string): ActionOption[] {
  if (type === 'cover') {
    return [
      { label: 'Open', value: 'open' },
      { label: 'Close', value: 'close' },
    ];
  }
  return [
    { label: 'Turn on', value: 'true' },
    { label: 'Turn off', value: 'false' },
  ];
}

@Component({
  selector: 'app-new-scene-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatIconModule, MatProgressSpinnerModule,
    FormsModule, ReactiveFormsModule,
  ],
  styles: [`
    .icon-picker-grid {
      display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px;
    }
    .icon-option {
      display: flex; align-items: center; justify-content: center;
      width: 40px; height: 40px; border-radius: 8px; cursor: pointer;
      border: 2px solid #e0e0e0; transition: all .15s;
    }
    .icon-option:hover { border-color: #00897B; background: #f0faf9; }
    .icon-option.selected { border-color: #00897B; background: #e0f2f1; }
  `],
  template: `
    <h2 mat-dialog-title>{{ data.scene ? 'Edit Scene' : 'New Scene' }}</h2>

    <mat-dialog-content style="min-width:480px; padding-top:8px;">

      <div *ngIf="loadingDevices" style="display:flex;flex-direction:column;align-items:center;gap:12px;padding:24px;">
        <mat-spinner diameter="36"></mat-spinner>
        <span style="font-size:13px;color:#757575;">Loading devices…</span>
      </div>

      <form *ngIf="!loadingDevices" [formGroup]="form"
            style="display:flex;flex-direction:column;gap:14px;">

        <!-- Name -->
        <mat-form-field appearance="outline">
          <mat-label>Scene name</mat-label>
          <input matInput formControlName="name" placeholder="e.g. Cozy Evening"
                 data-testid="scene-name-input">
          <mat-hint>Give your scene a descriptive name</mat-hint>
          <mat-error>Name is required</mat-error>
        </mat-form-field>

        <!-- Icon picker -->
        <div>
          <div style="font-size:14px;font-weight:500;color:#424242;margin-bottom:8px;">
            Choose an icon
          </div>
          <div class="icon-picker-grid">
            <div
              *ngFor="let icon of icons"
              class="icon-option"
              [class.selected]="selectedIcon === icon"
              (click)="selectedIcon = icon"
              [attr.data-testid]="'scene-icon-' + icon">
              <mat-icon>{{ icon }}</mat-icon>
            </div>
          </div>
        </div>

        <!-- Device actions -->
        <div>
          <div style="font-size:14px;font-weight:500;color:#424242;margin-bottom:8px;">
            Device actions
          </div>

          <div *ngIf="loadError" style="font-size:13px;color:#e53935;margin-bottom:8px;">
            Could not load devices. Check your connection and try again.
          </div>

          <div *ngIf="!loadError && allDevices.length === 0"
               style="font-size:13px;color:#9e9e9e;margin-bottom:8px;">
            No devices found. Add devices to your rooms first.
          </div>

          <div *ngFor="let pair of entryPairs; let i = index"
               style="display:flex;gap:8px;margin-bottom:8px;align-items:center;">
            <mat-form-field appearance="outline" style="flex:1;">
              <mat-label>Device</mat-label>
              <mat-select [(ngModel)]="pair.deviceId" [ngModelOptions]="{standalone: true}"
                          (ngModelChange)="onDeviceChange(pair)"
                          [attr.data-testid]="'scene-device-select-' + i">
                <mat-option *ngFor="let d of allDevices" [value]="d.id">
                  {{ d.name }}
                </mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline" style="flex:1;">
              <mat-label>Action</mat-label>
              <!-- pair.actions is a stable reference — never recreated unless device changes -->
              <mat-select [(ngModel)]="pair.actionValue" [ngModelOptions]="{standalone: true}"
                          [attr.data-testid]="'scene-action-select-' + i">
                <mat-option *ngFor="let a of pair.actions" [value]="a.value">
                  {{ a.label }}
                </mat-option>
              </mat-select>
            </mat-form-field>

            <button mat-icon-button (click)="removePair(i)"
                    style="color:#ef5350;" [attr.data-testid]="'scene-remove-entry-' + i">
              <mat-icon>remove_circle</mat-icon>
            </button>
          </div>

          <button mat-stroked-button (click)="addPair()" [disabled]="allDevices.length === 0"
                  data-testid="scene-add-device-button">
            <mat-icon>add</mat-icon> Add device
          </button>
        </div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary"
              [disabled]="!canSubmit()"
              (click)="submit()"
              data-testid="scene-submit-button">
        {{ data.scene ? 'Save' : 'Create Scene' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class NewSceneDialogComponent implements OnInit {
  icons = SCENE_ICONS;
  selectedIcon = 'auto_awesome';
  entryPairs: EntryPair[] = [];
  allDevices: DeviceOption[] = [];
  loadingDevices = true;
  loadError = false;
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private roomService: RoomService,
    private deviceService: DeviceService,
    public dialogRef: MatDialogRef<NewSceneDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { scene: SceneDto | null },
  ) {
    this.selectedIcon = this.data.scene?.icon ?? 'auto_awesome';
    this.form = this.fb.group({
      name: [this.data.scene?.name ?? '', Validators.required],
    });
  }

  /**
   * Loads all devices across all rooms, then pre-fills entries for edit mode.
   * catchError converts failures into an empty list so the subscribe next-handler
   * is always the single exit point, guaranteeing loadingDevices is always cleared.
   */
  ngOnInit(): void {
    this.roomService.getRooms().pipe(
      switchMap(rooms => {
        if (rooms.length === 0) {
          return of([] as any[][]);
        }
        // catchError per room so one failing room doesn't block the rest
        return forkJoin(
          rooms.map(r => this.deviceService.getDevices(r.id).pipe(catchError(() => of([]))))
        );
      }),
      catchError(() => {
        this.loadError = true;
        return of([] as any[][]);
      }),
    ).subscribe({
      next: deviceLists => {
        this.allDevices = (deviceLists as any[][])
          .flat()
          .map((d: any) => ({ id: d.id, name: d.name, type: d.type }));
        this.prefillEntries();
        this.loadingDevices = false;
      },
      error: () => {
        this.loadingDevices = false;
      },
    });
  }

  /** Adds an empty device-action row with pre-computed switch actions. */
  addPair(): void {
    const actions = actionsForType('switch');
    this.entryPairs.push({ deviceId: null, actionValue: actions[0].value, actions });
  }

  /** Removes a device-action row by index. */
  removePair(i: number): void {
    this.entryPairs.splice(i, 1);
  }

  /**
   * Updates the action list and resets the selected action
   * when the selected device changes.
   */
  onDeviceChange(pair: EntryPair): void {
    const device = this.allDevices.find(d => d.id === pair.deviceId);
    pair.actions = actionsForType(device ? device.type : 'switch');
    pair.actionValue = pair.actions[0].value;
  }

  /** Returns true when the form is valid and at least one entry is complete. */
  canSubmit(): boolean {
    return this.form.valid
      && this.entryPairs.length > 0
      && this.entryPairs.every(p => p.deviceId !== null && p.actionValue !== '');
  }

  /** Closes the dialog and emits the scene create/update request payload. */
  submit(): void {
    if (!this.canSubmit()) {
      return;
    }
    this.dialogRef.close({
      name: this.form.value['name'],
      icon: this.selectedIcon,
      entries: this.entryPairs.map(p => ({
        deviceId: p.deviceId,
        actionValue: p.actionValue,
      })),
    });
  }

  private prefillEntries(): void {
    if (this.data.scene) {
      this.entryPairs = this.data.scene.entries.map((e: SceneEntryDto) => {
        const device = this.allDevices.find(d => d.id === e.deviceId);
        return {
          deviceId: e.deviceId,
          actionValue: e.actionValue,
          actions: actionsForType(device ? device.type : 'switch'),
        };
      });
    } else {
      const actions = actionsForType('switch');
      this.entryPairs = [{ deviceId: null, actionValue: actions[0].value, actions }];
    }
  }
}
