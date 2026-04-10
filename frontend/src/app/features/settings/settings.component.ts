import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { MEMBERS } from '../../core/mock-data';
import { Member } from '../../core/models';
import { AuthService } from '../../core/auth.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { InviteMemberDialogComponent } from './invite-member-dialog.component';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatProgressBarModule,
    MatTabsModule, MatFormFieldModule, MatInputModule, MatDialogModule,
    MatSnackBarModule, FormsModule, ReactiveFormsModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Settings</h1>
        <p class="subtitle">Manage your profile and household members.</p>
      </div>

      <mat-tab-group color="primary" animationDuration="200ms">

        <!-- My Profile Tab -->
        <mat-tab label="My Profile">
          <div style="padding:24px 0;max-width:480px;">
            <div style="display:flex;flex-direction:column;align-items:flex-start;margin-bottom:24px;">
              <div class="avatar-large">AJ</div>
              <button mat-stroked-button style="font-size:13px;">Change Photo</button>
            </div>

            <form [formGroup]="profileForm" style="display:flex;flex-direction:column;gap:16px;">
              <mat-form-field appearance="outline">
                <mat-label>Display name</mat-label>
                <input matInput formControlName="displayName" placeholder="Your name">
                <mat-hint>This is how others will see you</mat-hint>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Email address</mat-label>
                <input matInput formControlName="email" [readonly]="true">
                <mat-icon matSuffix style="color:#9e9e9e;">lock</mat-icon>
                <mat-hint>Email cannot be changed</mat-hint>
              </mat-form-field>
            </form>

            <div style="margin-top:24px;border-top:1px solid #f0f0f0;padding-top:24px;">
              <div style="font-size:16px;font-weight:500;margin-bottom:16px;color:#212121;">Change Password</div>
              <form [formGroup]="passwordForm" style="display:flex;flex-direction:column;gap:16px;">
                <mat-form-field appearance="outline">
                  <mat-label>Current password</mat-label>
                  <input matInput type="password" formControlName="current" placeholder="Enter current password">
                </mat-form-field>
                <mat-form-field appearance="outline">
                  <mat-label>New password</mat-label>
                  <input matInput type="password" formControlName="newPw" placeholder="At least 8 characters" (input)="updateStrength()">
                  <mat-hint>Mix letters, numbers and symbols for a strong password</mat-hint>
                </mat-form-field>
                <div class="password-strength">
                  <div class="strength-bar" [class]="strengthLevel(0)"></div>
                  <div class="strength-bar" [class]="strengthLevel(1)"></div>
                  <div class="strength-bar" [class]="strengthLevel(2)"></div>
                </div>
                <div style="font-size:12px;color:#757575;">
                  Strength: {{ passwordStrength === 0 ? 'Too short' : passwordStrength === 1 ? 'Weak' : passwordStrength === 2 ? 'Medium' : 'Strong' }}
                </div>
                <mat-form-field appearance="outline">
                  <mat-label>Confirm new password</mat-label>
                  <input matInput type="password" formControlName="confirm" placeholder="Repeat new password">
                </mat-form-field>
                <div>
                  <button mat-flat-button color="primary" (click)="savePassword()">Update Password</button>
                </div>
              </form>
            </div>

            <div style="margin-top:16px;text-align:right;">
              <button mat-flat-button color="primary" (click)="saveProfile()">Save Changes</button>
            </div>
          </div>
        </mat-tab>

        <!-- Household Members Tab -->
        <mat-tab label="Household Members">
          <div style="padding:24px 0;max-width:600px;">
            <div style="display:flex;justify-content:flex-end;margin-bottom:16px;">
              <button mat-flat-button color="primary" (click)="openInvite()">
                <mat-icon>person_add</mat-icon> Invite Member
              </button>
            </div>

            <mat-card>
              <mat-card-content style="padding:0 16px;">
                <div class="member-item" *ngFor="let member of members">
                  <div class="avatar-circle">{{ member.avatarInitials }}</div>
                  <div class="member-info">
                    <h4>{{ member.name }}</h4>
                    <p>{{ member.email }}</p>
                  </div>
                  <span class="role-chip" [class.owner]="member.role === 'Owner'" [class.member]="member.role === 'Member'">
                    {{ member.role }}
                  </span>
                  <button
                    *ngIf="member.role !== 'Owner'"
                    mat-stroked-button
                    color="warn"
                    style="font-size:12px;"
                    (click)="revokeAccess(member)">
                    Revoke Access
                  </button>
                </div>
              </mat-card-content>
            </mat-card>
          </div>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
})
export class SettingsComponent implements OnInit {
  loading = true;
  members: Member[] = [];
  passwordStrength = 0;
  profileForm: FormGroup;
  passwordForm: FormGroup;

  constructor(private fb: FormBuilder, private dialog: MatDialog, private snackBar: MatSnackBar, private auth: AuthService) {
    this.profileForm = this.fb.group({
      displayName: [this.auth.currentUser?.name ?? ''],
      email: [{ value: this.auth.currentUser?.email ?? '', disabled: true }],
    });
    this.passwordForm = this.fb.group({
      current: [''],
      newPw: [''],
      confirm: [''],
    });
  }

  ngOnInit() {
    setTimeout(() => {
      this.members = MEMBERS.map(m => ({ ...m }));
      this.loading = false;
    }, 600);
  }

  updateStrength() {
    const pw = this.passwordForm.value.newPw ?? '';
    if (pw.length < 6) this.passwordStrength = 0;
    else if (pw.length < 8) this.passwordStrength = 1;
    else if (pw.length < 12 || !/[^a-zA-Z0-9]/.test(pw)) this.passwordStrength = 2;
    else this.passwordStrength = 3;
  }

  strengthLevel(bar: number): string {
    if (this.passwordStrength === 0) return '';
    if (this.passwordStrength === 1) return bar === 0 ? 'weak' : '';
    if (this.passwordStrength === 2) return bar <= 1 ? 'medium' : '';
    return 'strong';
  }

  saveProfile() { this.snackBar.open('Profile saved ✓', '', { duration: 2000 }); }

  savePassword() {
    const { newPw, confirm } = this.passwordForm.value;
    if (newPw !== confirm) {
      this.snackBar.open('Passwords do not match', '', { duration: 3000 });
      return;
    }
    this.snackBar.open('Password updated ✓', '', { duration: 2000 });
    this.passwordForm.reset();
    this.passwordStrength = 0;
  }

  revokeAccess(member: Member) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Revoke Access', message: `Remove ${member.name} from your household? They will lose access immediately.` }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.members = this.members.filter(m => m.id !== member.id);
        this.snackBar.open(`${member.name}'s access revoked ✓`, '', { duration: 2000 });
      }
    });
  }

  openInvite() {
    const ref = this.dialog.open(InviteMemberDialogComponent, { width: '400px' });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.members = [...this.members, {
          id: 'm_' + Date.now(),
          name: result.email.split('@')[0],
          email: result.email,
          role: result.role,
          avatarInitials: result.email.substring(0, 2).toUpperCase(),
        }];
        this.snackBar.open(`Invite sent to ${result.email} ✓`, '', { duration: 2000 });
      }
    });
  }
}
