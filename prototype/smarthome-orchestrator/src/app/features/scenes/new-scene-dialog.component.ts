import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Scene } from '../../core/models';

const SCENE_ICONS = [
  'wb_sunny','bedtime','movie','flight_takeoff','celebration',
  'weekend','kitchen','bed','auto_awesome','mode_night',
  'bolt','shield','eco','favorite','star',
  'home','local_florist','music_note','restaurant','sports_esports',
  'beach_access','directions_car','fitness_center','work','school',
];

@Component({
  selector: 'app-new-scene-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatIconModule, FormsModule, ReactiveFormsModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.scene ? 'Edit Scene' : 'New Scene' }}</h2>
    <mat-dialog-content style="min-width:460px;padding-top:8px;">
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;">
        <mat-form-field appearance="outline">
          <mat-label>Scene name</mat-label>
          <input matInput formControlName="name" placeholder="e.g. Cozy Evening">
          <mat-hint>Give your scene a descriptive name</mat-hint>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Short description</mat-label>
          <input matInput formControlName="description" placeholder="e.g. Dims lights · Closes blinds">
          <mat-hint>What does this scene do?</mat-hint>
        </mat-form-field>

        <div>
          <div style="font-size:14px;font-weight:500;color:#424242;margin-bottom:8px;">Choose an icon</div>
          <div class="icon-picker-grid">
            <div
              *ngFor="let icon of icons"
              class="icon-option"
              [class.selected]="selectedIcon === icon"
              (click)="selectedIcon = icon">
              <mat-icon>{{ icon }}</mat-icon>
            </div>
          </div>
        </div>

        <div>
          <div style="font-size:14px;font-weight:500;color:#424242;margin-bottom:8px;">Device actions</div>
          <div *ngFor="let pair of devicePairs; let i = index" style="display:flex;gap:8px;margin-bottom:8px;align-items:center;">
            <mat-form-field appearance="outline" style="flex:1;">
              <mat-label>Device</mat-label>
              <mat-select [(ngModel)]="pair.device" [ngModelOptions]="{standalone: true}">
                <mat-option value="Ceiling Light">Ceiling Light</mat-option>
                <mat-option value="TV">TV</mat-option>
                <mat-option value="Smart Blinds">Smart Blinds</mat-option>
                <mat-option value="AC Unit">AC Unit</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline" style="flex:1;">
              <mat-label>Action</mat-label>
              <mat-select [(ngModel)]="pair.action" [ngModelOptions]="{standalone: true}">
                <mat-option value="Turn on">Turn on</mat-option>
                <mat-option value="Turn off">Turn off</mat-option>
                <mat-option value="Dim 50%">Dim 50%</mat-option>
                <mat-option value="Open">Open</mat-option>
                <mat-option value="Close">Close</mat-option>
              </mat-select>
            </mat-form-field>
            <button mat-icon-button (click)="removePair(i)" style="color:#ef5350;">
              <mat-icon>remove_circle</mat-icon>
            </button>
          </div>
          <button mat-stroked-button (click)="addPair()">
            <mat-icon>add</mat-icon> Add device
          </button>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!form.valid" (click)="submit()">
        {{ data.scene ? 'Save' : 'Create Scene' }}
      </button>
    </mat-dialog-actions>
  `,
})
export class NewSceneDialogComponent {
  icons = SCENE_ICONS;
  selectedIcon = 'auto_awesome';
  devicePairs: { device: string; action: string }[] = [];
  form: any;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<NewSceneDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { scene: Scene | null }
  ) {
    this.selectedIcon = this.data.scene?.icon ?? 'auto_awesome';
    this.form = this.fb.group({
      name: [this.data.scene?.name ?? '', Validators.required],
      description: [this.data.scene?.description ?? '', Validators.required],
    });
  }

  addPair() { this.devicePairs.push({ device: '', action: '' }); }
  removePair(i: number) { this.devicePairs.splice(i, 1); }

  submit() {
    if (this.form.valid) {
      this.dialogRef.close({ ...this.form.value, icon: this.selectedIcon });
    }
  }
}
