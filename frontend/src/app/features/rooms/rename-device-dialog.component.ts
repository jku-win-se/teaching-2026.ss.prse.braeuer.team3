import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-rename-device-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule, FormsModule],
  template: `
    <h2 mat-dialog-title>Rename Device</h2>
    <mat-dialog-content style="min-width:340px; padding-top:8px;">
      <mat-form-field appearance="outline" style="width:100%;">
        <mat-label>New device name</mat-label>
        <input matInput [(ngModel)]="name" maxlength="50" (keydown.enter)="save()">
        <mat-hint>{{ name.length }}/50</mat-hint>
      </mat-form-field>
    </mat-dialog-content>
    <div style="display:flex;justify-content:flex-end;gap:8px;padding:8px 24px 16px;">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!name.trim()" (click)="save()">Rename</button>
    </div>
  `,
})
export class RenameDeviceDialogComponent {
  name: string;

  constructor(
    private dialogRef: MatDialogRef<RenameDeviceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { currentName: string }
  ) {
    this.name = data.currentName;
  }

  save() {
    if (this.name.trim()) {
      this.dialogRef.close(this.name.trim());
    }
  }
}
