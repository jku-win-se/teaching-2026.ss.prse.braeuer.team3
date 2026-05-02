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
import { AuthService } from '../../core/auth.service';
import { UserRole } from '../../core/auth.service';
import { MemberResponseDto } from '../../core/member.service';
import { MemberService } from '../../core/member.service';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { InviteMemberDialogComponent } from './invite-member-dialog.component';

interface MemberView {
  id: number;
  name: string;
  email: string;
  joinedAt: string;
  role: UserRole;
  avatarInitials: string;
}

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
        <p class="subtitle">{{ isOwner ? 'Manage your profile and household access.' : 'Manage your profile.' }}</p>
      </div>

      <mat-tab-group color="primary" animationDuration="200ms">

        <!-- My Profile Tab -->
        <mat-tab label="My Profile">
          <div style="padding:24px 0;max-width:480px;">
            <div style="display:flex;flex-direction:column;align-items:flex-start;margin-bottom:24px;">
              <div class="avatar-large">{{ auth.currentUser?.avatarInitials || 'U' }}</div>
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

        <!-- Household Access Tab -->
        <mat-tab label="Household Access" *ngIf="isOwner">
          <div style="padding:24px 0;max-width:600px;">
            <div style="display:flex;justify-content:flex-end;margin-bottom:16px;">
              <button mat-flat-button color="primary" (click)="openInvite()">
                <mat-icon>person_add</mat-icon> Invite Member
              </button>
            </div>

            <mat-card>
              <mat-card-content style="padding:0 16px;">
                <div *ngIf="members.length === 0" style="padding:28px 0;text-align:center;color:var(--text-muted);font-size:14px;">
                  No invited users yet.
                </div>
                <div class="member-item" *ngFor="let member of members">
                  <div class="avatar-circle">{{ member.avatarInitials }}</div>
                  <div class="member-info">
                    <h4>{{ member.name }}</h4>
                    <p>{{ member.email }}</p>
                  </div>
                  <span class="role-chip" [class.owner]="member.role === 'OWNER'" [class.member]="member.role === 'MEMBER'">
                    {{ member.role === 'OWNER' ? 'Owner' : 'Member' }}
                  </span>
                  <button
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
  members: MemberView[] = [];
  passwordStrength = 0;
  profileForm: FormGroup;
  passwordForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    public auth: AuthService,
    private memberService: MemberService,
  ) {
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
    if (!this.isOwner) {
      this.loading = false;
      return;
    }
    this.loadMembers();
  }

  get isOwner(): boolean {
    return this.auth.isOwner;
  }

  loadMembers() {
    this.loading = true;
    this.memberService.getMembers().subscribe({
      next: members => {
        this.members = members.map(m => this.toView(m));
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load members.', '', { duration: 3000 });
      }
    });
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

  revokeAccess(member: MemberView) {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Revoke Access', message: `Remove ${member.name} from your household? They will lose access immediately.` }
    });
    ref.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.memberService.removeMember(member.id).subscribe({
          next: () => {
            this.members = this.members.filter(m => m.id !== member.id);
            this.snackBar.open(`${member.name}'s access revoked ✓`, '', { duration: 2000 });
          },
          error: () => this.snackBar.open('Failed to revoke access.', '', { duration: 3000 }),
        });
      }
    });
  }

  openInvite() {
    const ref = this.dialog.open(InviteMemberDialogComponent, { width: '400px' });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.memberService.inviteMember(result.email, result.role).subscribe({
          next: member => {
            this.members = [...this.members, this.toView(member)];
            this.snackBar.open(`${member.email} can now access this home as ${member.role.toLowerCase()} ✓`, '', { duration: 2500 });
          },
          error: err => {
            const message = err.status === 404
              ? 'This email address is not registered.'
              : err.status === 409
                ? 'This user already belongs to a home.'
                : err.status === 400
                  ? 'You cannot invite yourself.'
                  : 'Failed to invite member.';
            this.snackBar.open(message, '', { duration: 3000 });
          }
        });
      }
    });
  }

  private toView(member: MemberResponseDto): MemberView {
    const name = member.name || member.email.split('@')[0];
    return {
      id: member.id,
      name,
      email: member.email,
      joinedAt: member.joinedAt,
      role: member.role,
      avatarInitials: name.substring(0, 2).toUpperCase(),
    };
  }
}
