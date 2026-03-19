import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatCheckboxModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="auth-page">
      <div class="auth-brand">
        <div class="brand-logo">
          <mat-icon>home_iot_device</mat-icon>
        </div>
        <h1>SmartHome</h1>
        <p>Control your home, anywhere.</p>
      </div>

      <mat-card class="auth-card">
        <mat-card-content>
          <h2 class="auth-title">Welcome back</h2>
          <p class="auth-subtitle">Sign in to your account</p>

          <form [formGroup]="form" (ngSubmit)="submit()" style="display:flex;flex-direction:column;gap:16px;margin-top:24px;">
            <mat-form-field appearance="outline">
              <mat-label>Email address</mat-label>
              <mat-icon matPrefix style="margin-right:8px;color:var(--text-muted);">mail_outline</mat-icon>
              <input matInput type="email" formControlName="email" placeholder="you@example.com" autocomplete="email">
              <mat-error *ngIf="form.get('email')?.hasError('required')">Email is required</mat-error>
              <mat-error *ngIf="form.get('email')?.hasError('email')">Enter a valid email</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Password</mat-label>
              <mat-icon matPrefix style="margin-right:8px;color:var(--text-muted);">lock_outline</mat-icon>
              <input matInput [type]="showPassword ? 'text' : 'password'" formControlName="password" autocomplete="current-password">
              <button type="button" mat-icon-button matSuffix (click)="showPassword = !showPassword" tabindex="-1">
                <mat-icon>{{ showPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-error *ngIf="form.get('password')?.hasError('required')">Password is required</mat-error>
            </mat-form-field>

            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:-8px;">
              <mat-checkbox formControlName="remember" color="primary">
                <span style="font-size:13px;color:var(--text-muted);">Remember me</span>
              </mat-checkbox>
              <a href="#" style="font-size:13px;color:var(--primary);text-decoration:none;font-weight:500;" (click)="$event.preventDefault()">
                Forgot password?
              </a>
            </div>

            <div *ngIf="errorMsg" style="background:#FEF2F2;border:1px solid #FECACA;border-radius:8px;padding:10px 14px;display:flex;align-items:center;gap:8px;">
              <mat-icon style="color:#EF4444;font-size:18px;width:18px;height:18px;">error_outline</mat-icon>
              <span style="font-size:13px;color:#B91C1C;">{{ errorMsg }}</span>
            </div>

            <button
              mat-flat-button
              color="primary"
              type="submit"
              [disabled]="loading"
              style="height:48px;border-radius:10px;font-size:15px;font-weight:600;margin-top:4px;"
            >
              <mat-spinner *ngIf="loading" diameter="20" style="display:inline-block;margin-right:8px;"></mat-spinner>
              {{ loading ? 'Signing in…' : 'Sign in' }}
            </button>
          </form>

          <div class="auth-divider">
            <span>or</span>
          </div>

          <button
            mat-stroked-button
            style="width:100%;height:44px;border-radius:10px;font-size:14px;font-weight:500;gap:8px;"
            (click)="demoLogin()"
          >
            <mat-icon style="font-size:18px;width:18px;height:18px;">play_circle_outline</mat-icon>
            Continue with demo account
          </button>

          <p style="text-align:center;margin-top:24px;font-size:14px;color:var(--text-muted);">
            Don't have an account?
            <a routerLink="/register" style="color:var(--primary);font-weight:600;text-decoration:none;"> Sign up</a>
          </p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-page {
      min-height: 100vh;
      background: linear-gradient(135deg, #0F172A 0%, #1E293B 50%, #0F172A 100%);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 24px;
    }

    .auth-brand {
      text-align: center;
      margin-bottom: 32px;
      color: white;
    }

    .brand-logo {
      width: 64px; height: 64px;
      border-radius: 18px;
      background: linear-gradient(135deg, var(--primary) 0%, #7C3AED 100%);
      display: flex; align-items: center; justify-content: center;
      margin: 0 auto 16px;
      box-shadow: 0 8px 32px rgba(79, 70, 229, 0.4);
    }

    .brand-logo mat-icon {
      color: white;
      font-size: 32px; width: 32px; height: 32px;
    }

    .auth-brand h1 {
      margin: 0 0 6px;
      font-size: 28px; font-weight: 800;
      letter-spacing: -0.5px;
    }

    .auth-brand p {
      margin: 0;
      font-size: 14px;
      opacity: 0.6;
    }

    .auth-card {
      width: 100%;
      max-width: 420px;
      border-radius: 20px !important;
      box-shadow: 0 24px 64px rgba(0,0,0,0.4) !important;
      border: 1px solid rgba(255,255,255,0.06);
    }

    mat-card-content { padding: 32px !important; }

    .auth-title {
      margin: 0 0 4px;
      font-size: 22px; font-weight: 700;
      color: var(--text);
    }

    .auth-subtitle {
      margin: 0;
      font-size: 14px;
      color: var(--text-muted);
    }

    .auth-divider {
      display: flex; align-items: center; gap: 12px;
      margin: 20px 0;
      color: var(--text-muted);
      font-size: 13px;
    }
    .auth-divider::before, .auth-divider::after {
      content: ''; flex: 1;
      height: 1px; background: var(--border);
    }
  `],
})
export class LoginComponent {
  form: FormGroup;
  showPassword = false;
  loading = false;
  errorMsg = '';

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      remember: [false],
    });
  }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading = true;
    this.errorMsg = '';
    setTimeout(() => {
      const ok = this.auth.login(this.form.value.email, this.form.value.password);
      if (ok) {
        this.router.navigate(['/dashboard']);
      } else {
        this.errorMsg = 'Invalid email or password.';
        this.loading = false;
      }
    }, 800);
  }

  demoLogin() {
    this.loading = true;
    setTimeout(() => {
      this.auth.login('alex@smarthome.demo', 'demo');
      this.router.navigate(['/dashboard']);
    }, 600);
  }
}
