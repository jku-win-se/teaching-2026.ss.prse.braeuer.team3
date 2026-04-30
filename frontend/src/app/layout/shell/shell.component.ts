import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { RealtimeService } from '../../core/realtime.service';
import { RuleNotificationDto } from '../../core/models';

interface NavItem {
  label: string;
  icon: string;
  route: string;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatSidenavModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatBadgeModule,
    MatSnackBarModule,
  ],
  template: `
    <mat-sidenav-container style="height:100vh;background:var(--bg);">
      <mat-sidenav #sidenav class="app-sidenav" [mode]="isMobile ? 'over' : 'side'" [opened]="!isMobile">
        <div class="sidenav-logo">
          <div class="logo-icon"><mat-icon>home_iot_device</mat-icon></div>
          <span>SmartHome</span>
        </div>

        <div *ngFor="let group of navGroups">
          <div class="nav-group-label">{{ group.label }}</div>
          <a
            *ngFor="let item of group.items"
            class="nav-link"
            [routerLink]="item.route"
            routerLinkActive="active"
            (click)="isMobile && sidenav.close()"
          >
            <mat-icon>{{ item.icon }}</mat-icon>
            {{ item.label }}
          </a>
        </div>

        <div class="sidenav-footer">
          <button mat-button class="logout-btn" (click)="logout()">
            <mat-icon>logout</mat-icon>
            Sign out
          </button>
        </div>
      </mat-sidenav>

      <mat-sidenav-content>
        <mat-toolbar class="app-toolbar">
          <button mat-icon-button *ngIf="isMobile" (click)="sidenav.toggle()" style="color:white;">
            <mat-icon>menu</mat-icon>
          </button>
          <span *ngIf="isMobile" style="font-weight:700;font-size:15px;color:white;letter-spacing:-0.3px;">SmartHome</span>
          <span class="toolbar-spacer"></span>
          <button mat-icon-button style="color:rgba(255,255,255,0.7);">
            <mat-icon>search</mat-icon>
          </button>
          <button mat-icon-button style="color:rgba(255,255,255,0.7);" [matMenuTriggerFor]="notificationMenu">
            <mat-icon
              [matBadge]="notifications.length"
              [matBadgeHidden]="notifications.length === 0"
              matBadgeColor="warn"
              matBadgeSize="small"
            >notifications</mat-icon>
          </button>

          <mat-menu #notificationMenu="matMenu" xPosition="before">
            <div style="min-width:300px;max-width:360px;">
              <div style="padding:12px 16px 8px;border-bottom:1px solid #F1F5F9;display:flex;justify-content:space-between;align-items:center;pointer-events:none;">
                <span style="font-weight:600;font-size:14px;color:var(--text);">Benachrichtigungen</span>
                <span style="font-size:12px;color:var(--text-muted);">{{ notifications.length }} neu</span>
              </div>
              <div style="max-height:320px;overflow-y:auto;">
                <div *ngIf="notifications.length === 0" style="padding:24px 16px;color:var(--text-muted);font-size:13px;text-align:center;pointer-events:none;">
                  Keine Benachrichtigungen
                </div>
                <div *ngFor="let n of notifications" style="padding:10px 16px;border-bottom:1px solid #F8FAFC;display:flex;gap:10px;align-items:flex-start;pointer-events:none;">
                  <mat-icon [style.color]="n.success ? '#22C55E' : '#EF4444'" style="font-size:18px;width:18px;height:18px;flex-shrink:0;margin-top:2px;">{{ n.success ? 'check_circle' : 'error' }}</mat-icon>
                  <div>
                    <div style="font-size:13px;font-weight:500;color:var(--text);">{{ n.ruleName }}</div>
                    <div style="font-size:12px;color:var(--text-muted);">{{ n.message }}</div>
                  </div>
                </div>
              </div>
              <div *ngIf="notifications.length > 0" style="padding:6px 16px;border-top:1px solid #F1F5F9;">
                <button mat-button color="primary" style="font-size:12px;" (click)="clearNotifications()">Alle löschen</button>
              </div>
            </div>
          </mat-menu>

          <div class="toolbar-avatar" style="margin-left:4px;cursor:pointer;" [matMenuTriggerFor]="userMenu">
            <div class="avatar-sm">{{ auth.currentUser?.avatarInitials || 'A' }}</div>
            <span *ngIf="!isMobile">{{ auth.currentUser?.name || 'User' }}</span>
          </div>
          <mat-menu #userMenu="matMenu">
            <div style="padding:12px 16px 8px;border-bottom:1px solid #F1F5F9;margin-bottom:4px;pointer-events:none;">
              <div style="font-weight:600;font-size:14px;color:var(--text);">{{ auth.currentUser?.name }}</div>
              <div style="font-size:12px;color:var(--text-muted);">{{ auth.currentUser?.email }}</div>
            </div>
            <button mat-menu-item routerLink="/settings">
              <mat-icon>settings</mat-icon> Settings
            </button>
            <button mat-menu-item (click)="logout()">
              <mat-icon>logout</mat-icon> Sign out
            </button>
          </mat-menu>
        </mat-toolbar>

        <router-outlet></router-outlet>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
})
export class ShellComponent implements OnInit, OnDestroy {
  @ViewChild('sidenav') sidenav!: MatSidenav;
  isMobile = false;
  notifications: RuleNotificationDto[] = [];

  private notificationSub?: Subscription;

  navGroups: NavGroup[] = [
    {
      label: 'Home',
      items: [{ label: 'Dashboard', icon: 'dashboard', route: '/dashboard' }]
    },
    {
      label: 'Devices',
      items: [
        { label: 'Rooms & Devices', icon: 'devices', route: '/rooms' },
        { label: 'Scenes', icon: 'auto_awesome', route: '/scenes' },
      ]
    },
    {
      label: 'Automation',
      items: [
        { label: 'Rules', icon: 'rule', route: '/rules' },
        { label: 'Schedules', icon: 'schedule', route: '/schedules' },
      ]
    },
    {
      label: 'Insights',
      items: [
        { label: 'Energy', icon: 'bolt', route: '/energy' },
        { label: 'Activity Log', icon: 'history', route: '/log' },
      ]
    },
    {
      label: 'Account',
      items: [{ label: 'Settings', icon: 'settings', route: '/settings' }]
    },
  ];

  constructor(
    private router: Router,
    private bp: BreakpointObserver,
    public auth: AuthService,
    private realtime: RealtimeService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.bp.observe(['(max-width: 768px)']).subscribe(result => {
      this.isMobile = result.matches;
    });

    this.notificationSub = this.realtime.ruleNotifications$.subscribe(n => {
      this.notifications.unshift(n);
      const icon = n.success ? '✓' : '✗';
      this.snackBar.open(`${icon} ${n.ruleName}: ${n.message}`, '', {
        duration: n.success ? 3000 : 5000,
      });
    });
  }

  ngOnDestroy() {
    this.notificationSub?.unsubscribe();
  }

  clearNotifications() {
    this.notifications = [];
  }

  logout() {
    this.auth.logout();
  }
}
