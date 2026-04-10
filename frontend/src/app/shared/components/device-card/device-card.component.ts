import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSliderModule } from '@angular/material/slider';
import { MatMenuModule } from '@angular/material/menu';
import { FormsModule } from '@angular/forms';
import { Device, Room } from '../../../core/models';

@Component({
  selector: 'app-device-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatSliderModule,
    MatMenuModule,
    FormsModule,
  ],
  template: `
    <mat-card style="height:100%;">
      <mat-card-content style="padding:16px;">
        <div style="display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:12px;">
          <div style="display:flex;align-items:center;gap:12px;">
            <div class="device-icon-wrap">
              <mat-icon>{{ device.icon }}</mat-icon>
            </div>
            <div>
              <div style="font-size:15px;font-weight:500;color:#212121;">{{ device.name }}</div>
              <div style="font-size:12px;color:#9e9e9e;">{{ room?.name }} · {{ device.type | titlecase }}</div>
            </div>
          </div>
          <button mat-icon-button [matMenuTriggerFor]="menu" style="margin:-8px -8px 0 0;">
            <mat-icon>more_vert</mat-icon>
          </button>
          <mat-menu #menu="matMenu">
            <button mat-menu-item (click)="rename.emit()">
              <mat-icon>edit</mat-icon> Rename
            </button>
            <button mat-menu-item style="color:#ef5350;" (click)="remove.emit()">
              <mat-icon style="color:#ef5350;">delete</mat-icon> Remove
            </button>
          </mat-menu>
        </div>

        <!-- Switch -->
        <ng-container *ngIf="device.type === 'switch'">
          <div style="display:flex;align-items:center;gap:12px;margin-top:8px;">
            <mat-slide-toggle
              [checked]="device.state.on ?? false"
              (change)="toggleSwitch($event.checked)"
              color="primary">
            </mat-slide-toggle>
            <span style="font-size:13px;font-weight:500;" [style.color]="device.state.on ? '#00897B' : '#9e9e9e'">
              {{ device.state.on ? 'ON' : 'OFF' }}
            </span>
          </div>
        </ng-container>

        <!-- Dimmer -->
        <ng-container *ngIf="device.type === 'dimmer'">
          <div style="display:flex;align-items:center;gap:12px;margin-top:4px;">
            <mat-slide-toggle
              [checked]="device.state.on ?? false"
              (change)="toggleSwitch($event.checked)"
              color="primary">
            </mat-slide-toggle>
            <span style="font-size:13px;font-weight:500;" [style.color]="device.state.on ? '#00897B' : '#9e9e9e'">
              {{ device.state.on ? 'ON' : 'OFF' }}
            </span>
          </div>
          <div style="margin-top:12px;">
            <div style="font-size:12px;color:#9e9e9e;margin-bottom:4px;">Brightness: {{ device.state.brightness }}%</div>
            <mat-slider min="0" max="100" step="5" style="width:100%;" color="primary" discrete>
              <input matSliderThumb
                [value]="device.state.brightness ?? 0"
                (valueChange)="brightnessChange($event)">
            </mat-slider>
          </div>
        </ng-container>

        <!-- Thermostat -->
        <ng-container *ngIf="device.type === 'thermostat'">
          <div class="thermostat-controls">
            <button mat-icon-button (click)="changeTemp(-1)">
              <mat-icon>remove</mat-icon>
            </button>
            <div class="temp-display">{{ device.state.temperature }}°C</div>
            <button mat-icon-button (click)="changeTemp(1)">
              <mat-icon>add</mat-icon>
            </button>
          </div>
          <div style="margin-top:8px;display:flex;align-items:center;gap:8px;">
            <mat-slide-toggle [checked]="device.state.on ?? false" (change)="toggleSwitch($event.checked)" color="primary"></mat-slide-toggle>
            <span style="font-size:12px;color:#9e9e9e;">{{ device.state.on ? 'Running' : 'Standby' }}</span>
          </div>
        </ng-container>

        <!-- Sensor -->
        <ng-container *ngIf="device.type === 'sensor'">
          <div class="sensor-display">
            <div class="sensor-value">{{ device.state.sensorValue }} <span style="font-size:14px;color:#9e9e9e;">{{ device.state.sensorUnit }}</span></div>
            <button mat-stroked-button color="primary" style="font-size:12px;" (click)="injectValue.emit()">
              <mat-icon style="font-size:16px;width:16px;height:16px;vertical-align:middle;">science</mat-icon>
              Inject Value
            </button>
          </div>
        </ng-container>

        <!-- Cover -->
        <ng-container *ngIf="device.type === 'cover'">
          <div style="margin-top:8px;">
            <div style="font-size:12px;color:#9e9e9e;margin-bottom:4px;">
              Position: {{ device.state.coverPosition === 0 ? 'Closed' : device.state.coverPosition === 100 ? 'Open' : device.state.coverPosition + '%' }}
            </div>
            <div class="cover-buttons">
              <button mat-stroked-button color="primary" (click)="coverAction.emit('open')" style="flex:1;">Open</button>
              <button mat-stroked-button (click)="coverAction.emit('stop')" style="flex:1;">Stop</button>
              <button mat-stroked-button (click)="coverAction.emit('close')" style="flex:1;">Close</button>
            </div>
          </div>
        </ng-container>
      </mat-card-content>
    </mat-card>
  `,
})
export class DeviceCardComponent {
  @Input() device!: Device;
  @Input() room?: Room;
  @Output() toggled = new EventEmitter<boolean>();
  @Output() sliderChanged = new EventEmitter<number>();
  @Output() tempChanged = new EventEmitter<number>();
  @Output() coverAction = new EventEmitter<string>();
  @Output() injectValue = new EventEmitter<void>();
  @Output() rename = new EventEmitter<void>();
  @Output() remove = new EventEmitter<void>();

  toggleSwitch(on: boolean) {
    this.device.state = { ...this.device.state, on };
    this.toggled.emit(on);
  }

  brightnessChange(val: number) {
    this.device.state = { ...this.device.state, brightness: val };
    this.sliderChanged.emit(val);
  }

  changeTemp(delta: number) {
    const t = (this.device.state.temperature ?? 20) + delta;
    this.device.state = { ...this.device.state, temperature: Math.max(10, Math.min(35, t)) };
    this.tempChanged.emit(this.device.state.temperature);
  }
}
