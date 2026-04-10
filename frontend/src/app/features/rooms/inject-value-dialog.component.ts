import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-inject-value-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, FormsModule],
  template: `
    <h2 mat-dialog-title>Inject Sensor Value</h2>
    <mat-dialog-content style="min-width:320px; padding-top:8px;">
      <mat-form-field appearance="outline" style="width:100%;">
        <mat-label>Sensor value</mat-label>
        <input matInput type="number" [(ngModel)]="value" (keydown.enter)="save()">
      </mat-form-field>
    </mat-dialog-content>
    <div style="display:flex;justify-content:flex-end;gap:8px;padding:8px 24px 16px;">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" (click)="save()">Inject</button>
    </div>
  `,
})
export class InjectValueDialogComponent {
  value: number = 0;

  constructor(private dialogRef: MatDialogRef<InjectValueDialogComponent>) {}

  save() {
    this.dialogRef.close(this.value);
  }
}
