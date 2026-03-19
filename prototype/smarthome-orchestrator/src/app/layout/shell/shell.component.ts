import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatSidenavModule, MatSidenav } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { BreakpointObserver } from '@angular/cdk/layout';
import { AuthService } from '../../core/auth.service';

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
          <button mat-icon-button style="color:rgba(255,255,255,0.7);position:relative;">
            <mat-icon>notifications</mat-icon>
            <span style="
              position:absolute; top:6px; right:6px;
              width:16px; height:16px; border-radius:50%;
              background:#EF4444; color:white;
              font-size:10px; font-weight:700; line-height:16px;
              text-align:center; pointer-events:none;
            ">3</span>
          </button>
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
export class ShellComponent implements OnInit {
  @ViewChild('sidenav') sidenav!: MatSidenav;
  isMobile = false;

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

  constructor(private router: Router, private bp: BreakpointObserver, public auth: AuthService) {}

  ngOnInit() {
    this.bp.observe(['(max-width: 768px)']).subscribe(result => {
      this.isMobile = result.matches;
    });
  }

  logout() {
    this.auth.logout();
  }
}
