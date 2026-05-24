import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subscription, interval } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { AuthService } from '../../core/auth.service';
import { UserRole } from '../../core/auth.service';
import { MemberResponseDto } from '../../core/member.service';
import { MemberService } from '../../core/member.service';
import { MqttService, MqttConfig, MqttMessage } from '../../core/mqtt.service';
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
    MatTabsModule, MatFormFieldModule, MatInputModule, MatChipsModule, MatDialogModule,
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

        <!-- MQTT Integration Tab (owner only, US-019) -->
        <mat-tab label="MQTT Integration" *ngIf="isOwner">
          <div style="padding:24px 0;max-width:560px;">

            <!-- Status chip -->
            <div style="display:flex;align-items:center;gap:12px;margin-bottom:24px;">
              <div class="mqtt-status-dot" [class.connected]="mqttConnected"></div>
              <span style="font-size:15px;font-weight:500;color:#212121;">
                {{ mqttConnected ? 'Verbunden (simuliert)' : 'Getrennt' }}
              </span>
              <span style="font-size:12px;color:#9e9e9e;margin-left:4px;">(kein echter Broker)</span>
            </div>

            <!-- Configuration form -->
            <mat-card style="margin-bottom:20px;">
              <mat-card-content style="padding:16px;">
                <div style="font-size:14px;font-weight:500;margin-bottom:12px;color:#616161;">
                  Broker-Konfiguration
                </div>
                <form [formGroup]="mqttForm" style="display:flex;flex-direction:column;gap:12px;">
                  <mat-form-field appearance="outline">
                    <mat-label>Broker-URL</mat-label>
                    <input matInput formControlName="brokerUrl"
                           placeholder="z.B. mqtt://localhost:1883"
                           data-testid="mqtt-broker-url-input">
                    <mat-hint>Nur zur Konfiguration gespeichert — es wird kein echter Broker kontaktiert.</mat-hint>
                    <mat-error *ngIf="mqttForm.get('brokerUrl')?.hasError('required')">
                      Broker-URL ist erforderlich
                    </mat-error>
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>Basis-Topic</mat-label>
                    <input matInput formControlName="topic"
                           placeholder="z.B. smarthome"
                           data-testid="mqtt-topic-input">
                    <mat-hint>Gerätezustände werden unter [topic]/devices/[id] publiziert.</mat-hint>
                    <mat-error *ngIf="mqttForm.get('topic')?.hasError('required')">
                      Topic ist erforderlich
                    </mat-error>
                  </mat-form-field>
                  <div style="display:flex;gap:8px;flex-wrap:wrap;">
                    <button mat-stroked-button color="primary"
                            (click)="saveMqttConfig()"
                            [disabled]="mqttForm.invalid"
                            data-testid="mqtt-save-button">
                      <mat-icon>save</mat-icon> Speichern
                    </button>
                    <button mat-flat-button color="primary"
                            *ngIf="!mqttConnected"
                            (click)="connectMqtt()"
                            [disabled]="!mqttConfigSaved"
                            data-testid="mqtt-connect-button">
                      <mat-icon>wifi</mat-icon> Verbinden
                    </button>
                    <button mat-flat-button color="warn"
                            *ngIf="mqttConnected"
                            (click)="disconnectMqtt()"
                            data-testid="mqtt-disconnect-button">
                      <mat-icon>wifi_off</mat-icon> Trennen
                    </button>
                  </div>
                </form>
              </mat-card-content>
            </mat-card>

            <!-- Message log -->
            <mat-card>
              <mat-card-content style="padding:16px;">
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
                  <span style="font-size:14px;font-weight:500;color:#616161;">
                    Simuliertes Nachrichtenprotokoll
                  </span>
                  <button mat-icon-button
                          title="Protokoll löschen"
                          (click)="clearMqttMessages()"
                          [disabled]="mqttMessages.length === 0"
                          data-testid="mqtt-clear-log-button">
                    <mat-icon>delete_outline</mat-icon>
                  </button>
                </div>
                <div *ngIf="mqttMessages.length === 0"
                     style="text-align:center;padding:20px 0;color:var(--text-muted);font-size:13px;">
                  Noch keine MQTT-Nachrichten. Verbinde dich und ändere einen Gerätezustand.
                </div>
                <div class="mqtt-log" *ngIf="mqttMessages.length > 0">
                  <div class="mqtt-log-entry" *ngFor="let msg of mqttMessages"
                       data-testid="mqtt-log-entry">
                    <span class="mqtt-ts">{{ msg.timestamp }}</span>
                    <span class="mqtt-dir" [class.publish]="msg.direction === 'PUBLISH'"
                          [class.system]="msg.direction === 'SYSTEM'">
                      {{ msg.direction }}
                    </span>
                    <span class="mqtt-topic">{{ msg.topic }}</span>
                    <span class="mqtt-payload">{{ msg.payload }}</span>
                  </div>
                </div>
              </mat-card-content>
            </mat-card>

          </div>
        </mat-tab>

      </mat-tab-group>
    </div>
  `,
  styles: [`
    .mqtt-status-dot {
      width: 12px; height: 12px; border-radius: 50%;
      background: #bdbdbd; flex-shrink: 0;
    }
    .mqtt-status-dot.connected { background: #4caf50; }
    .mqtt-log {
      font-family: monospace; font-size: 12px;
      max-height: 280px; overflow-y: auto;
      background: #fafafa; border: 1px solid #e0e0e0;
      border-radius: 4px; padding: 8px;
      display: flex; flex-direction: column; gap: 4px;
    }
    .mqtt-log-entry {
      display: flex; gap: 8px; align-items: baseline; flex-wrap: wrap;
    }
    .mqtt-ts { color: #9e9e9e; min-width: 64px; }
    .mqtt-dir { font-weight: 700; min-width: 60px; color: #757575; }
    .mqtt-dir.publish { color: #1976d2; }
    .mqtt-dir.system  { color: #f57c00; }
    .mqtt-topic { color: #388e3c; }
    .mqtt-payload { color: #424242; word-break: break-all; }
  `]
})
export class SettingsComponent implements OnInit, OnDestroy {
  loading = true;
  members: MemberView[] = [];
  passwordStrength = 0;
  profileForm: FormGroup;
  passwordForm: FormGroup;

  // MQTT state (US-019)
  mqttForm: FormGroup;
  mqttConnected = false;
  mqttConfigSaved = false;
  mqttMessages: MqttMessage[] = [];
  private mqttPollSub?: Subscription;

  constructor(
    private fb: FormBuilder,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    public auth: AuthService,
    private memberService: MemberService,
    private mqttService: MqttService,
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
    this.mqttForm = this.fb.group({
      brokerUrl: ['', Validators.required],
      topic: ['smarthome', Validators.required],
    });
  }

  ngOnInit() {
    if (!this.isOwner) {
      this.loading = false;
      return;
    }
    this.loadMembers();
    this.loadMqttConfig();
  }

  ngOnDestroy() {
    this.mqttPollSub?.unsubscribe();
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

  // ── MQTT methods (US-019) ──────────────────────────────────────────────────

  loadMqttConfig() {
    this.mqttService.getConfig().subscribe({
      next: (config: MqttConfig) => {
        this.mqttForm.patchValue({ brokerUrl: config.brokerUrl, topic: config.topic });
        this.mqttConnected = config.connected;
        this.mqttConfigSaved = !!config.brokerUrl;
        if (config.connected) {
          this.startMqttPolling();
        }
      },
      error: () => { /* MQTT config load failure is non-critical */ }
    });
  }

  saveMqttConfig() {
    const { brokerUrl, topic } = this.mqttForm.value;
    this.mqttService.saveConfig(brokerUrl, topic).subscribe({
      next: (config: MqttConfig) => {
        this.mqttConfigSaved = true;
        this.mqttConnected = config.connected;
        this.snackBar.open('MQTT-Konfiguration gespeichert ✓', '', { duration: 2500 });
      },
      error: () => this.snackBar.open('Fehler beim Speichern der MQTT-Konfiguration.', '', { duration: 3000 }),
    });
  }

  connectMqtt() {
    this.mqttService.connect().subscribe({
      next: (config: MqttConfig) => {
        this.mqttConnected = config.connected;
        this.snackBar.open('MQTT-Verbindung hergestellt (simuliert) ✓', '', { duration: 2500 });
        this.startMqttPolling();
      },
      error: (err: { error?: { message?: string } }) => {
        const msg = err?.error?.message ?? 'Verbindung fehlgeschlagen.';
        this.snackBar.open(msg, '', { duration: 3000 });
      },
    });
  }

  disconnectMqtt() {
    this.mqttService.disconnect().subscribe({
      next: () => {
        this.mqttConnected = false;
        this.mqttPollSub?.unsubscribe();
        this.snackBar.open('MQTT-Verbindung getrennt.', '', { duration: 2000 });
        this.loadMqttMessages();
      },
      error: () => this.snackBar.open('Trennen fehlgeschlagen.', '', { duration: 3000 }),
    });
  }

  clearMqttMessages() {
    this.mqttService.clearMessages().subscribe({
      next: () => { this.mqttMessages = []; },
      error: () => { /* ignore */ }
    });
  }

  private loadMqttMessages() {
    this.mqttService.getMessages().subscribe({
      next: (msgs: MqttMessage[]) => { this.mqttMessages = msgs; },
      error: () => { /* non-critical */ }
    });
  }

  private startMqttPolling() {
    this.mqttPollSub?.unsubscribe();
    this.mqttPollSub = interval(3000).pipe(
      switchMap(() => this.mqttService.getMessages())
    ).subscribe({
      next: (msgs: MqttMessage[]) => { this.mqttMessages = msgs; },
      error: () => { /* polling errors are non-critical */ }
    });
    this.loadMqttMessages();
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
