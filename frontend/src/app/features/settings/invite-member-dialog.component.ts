import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-invite-member-dialog',
  standalone: true,
  imports: [
    CommonModule, MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, FormsModule, ReactiveFormsModule,
  ],
  template: `
    <h2 mat-dialog-title>Invite User</h2>
    <mat-dialog-content style="padding-top:8px;min-width:340px;">
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;">
        <mat-form-field appearance="outline">
          <mat-label>Email address</mat-label>
          <input matInput type="email" formControlName="email" placeholder="name@example.com">
          <mat-hint>The account must already be registered</mat-hint>
          <mat-error *ngIf="form.get('email')?.hasError('required')">Email is required</mat-error>
          <mat-error *ngIf="form.get('email')?.hasError('email')">Enter a valid email address</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Role</mat-label>
          <mat-select formControlName="role">
            <mat-option value="MEMBER">Member</mat-option>
            <mat-option value="OWNER">Owner</mat-option>
          </mat-select>
          <mat-hint>Owners can manage rooms, devices, rules, schedules, and users</mat-hint>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-flat-button color="primary" [disabled]="!form.valid" (click)="submit()">Send Invite</button>
    </mat-dialog-actions>
  `,
})
export class InviteMemberDialogComponent {
  form: FormGroup;

  constructor(private fb: FormBuilder, private dialogRef: MatDialogRef<InviteMemberDialogComponent>) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      role: ['MEMBER', Validators.required],
    });
  }

  submit() {
    if (this.form.valid) this.dialogRef.close(this.form.value);
  }
}
