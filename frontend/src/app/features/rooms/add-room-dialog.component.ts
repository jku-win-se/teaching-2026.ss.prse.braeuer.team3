import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';

const ROOM_ICONS = [
  { value: 'weekend',        label: 'Living Room' },
  { value: 'kitchen',        label: 'Kitchen' },
  { value: 'bed',            label: 'Bedroom' },
  { value: 'door_front',     label: 'Hallway' },
  { value: 'garage',         label: 'Garage' },
  { value: 'bathroom',       label: 'Bathroom' },
  { value: 'child_care',     label: 'Kids Room' },
  { value: 'home_work',      label: 'Office' },
  { value: 'outdoor_grill',  label: 'Garden' },
  { value: 'dining',         label: 'Dining Room' },
  { value: 'fitness_center', label: 'Gym' },
  { value: 'stairs',         label: 'Other' },
];

@Component({
  selector: 'app-add-room-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatIconModule, FormsModule],
  styles: [`
    .icon-grid {
      display: grid;
      grid-template-columns: repeat(6, 1fr);
      gap: 8px;
      margin-top: 4px;
    }
    .icon-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
      padding: 10px 4px;
      border: 2px solid #e0e0e0;
      border-radius: 10px;
      cursor: pointer;
      font-size: 11px;
      color: #616161;
      background: #fff;
      transition: all .15s;
      text-align: center;
      line-height: 1.2;
    }
    .icon-btn:hover { border-color: #00897B; background: #f0faf9; }
    .icon-btn.selected { border-color: #00897B; background: #e0f2f1; color: #00897B; font-weight: 500; }
    .icon-btn mat-icon { font-size: 22px; width: 22px; height: 22px; }
    .actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 16px; }
  `],
  template: `
    <h2 mat-dialog-title>Add New Room</h2>
    <mat-dialog-content style="min-width:400px; padding-top:8px;">
      <mat-form-field appearance="outline" style="width:100%; margin-bottom:20px;">
        <mat-label>Room name</mat-label>
        <input matInput [(ngModel)]="name" placeholder="e.g. Guest Room" maxlength="30">
        <mat-hint>{{ name.length }}/30</mat-hint>
      </mat-form-field>

      <p style="font-size:13px; font-weight:500; color:#424242; margin:0 0 10px;">Icon</p>
      <div class="icon-grid">
        <div *ngFor="let icon of icons"
             class="icon-btn"
             [class.selected]="selectedIcon === icon.value"
             (click)="selectedIcon = icon.value">
          <mat-icon>{{ icon.value }}</mat-icon>
          <span>{{ icon.label }}</span>
        </div>
      </div>
    </mat-dialog-content>
    <div class="actions" style="padding: 8px 24px 16px;">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!name.trim() || !selectedIcon" (click)="save()">Add Room</button>
    </div>
  `,
})
export class AddRoomDialogComponent {
  name = '';
  selectedIcon = 'weekend';
  readonly icons = ROOM_ICONS;

  constructor(private dialogRef: MatDialogRef<AddRoomDialogComponent>) {}

  save() {
    this.dialogRef.close({ name: this.name.trim(), icon: this.selectedIcon });
  }
}
