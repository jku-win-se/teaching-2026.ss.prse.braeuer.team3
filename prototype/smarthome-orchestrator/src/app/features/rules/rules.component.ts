import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RULES } from '../../core/mock-data';
import { Rule, TriggerType } from '../../core/models';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { NewRuleDialogComponent } from './new-rule-dialog.component';

@Component({
  selector: 'app-rules',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatSlideToggleModule,
    MatProgressBarModule, MatDialogModule, MatSnackBarModule, MatStepperModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, FormsModule, ReactiveFormsModule,
    EmptyStateComponent,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Automation Rules</h1>
        <p class="subtitle">Rules automatically control your devices based on time, sensor readings, or device events.</p>
      </div>

      <div class="rules-grid" *ngIf="rules.length > 0; else emptyRules">
        <mat-card *ngFor="let rule of rules">
          <div class="rule-card-content">
            <div class="rule-header">
              <h3>{{ rule.name }}</h3>
              <mat-slide-toggle [(ngModel)]="rule.active" color="primary" (change)="onToggleRule(rule)"></mat-slide-toggle>
            </div>
            <p class="rule-summary">{{ rule.summary }}</p>
            <div class="rule-chips">
              <span class="trigger-chip">
                <span *ngIf="rule.triggerType === 'time'">🕐 Time</span>
                <span *ngIf="rule.triggerType === 'threshold'">📊 Threshold</span>
                <span *ngIf="rule.triggerType === 'event'">⚡ Event</span>
              </span>
              <span *ngIf="rule.hasConflict" class="conflict-chip">⚠ Conflict detected</span>
              <span *ngIf="!rule.active" style="font-size:12px;color:#9e9e9e;">Inactive</span>
            </div>
          </div>
        </mat-card>
      </div>

      <ng-template #emptyRules>
        <app-empty-state
          icon="rule"
          title="No rules yet"
          subtitle="Create your first automation rule to control devices automatically."
          actionLabel="+ New Rule"
          (action)="openNewRule()">
        </app-empty-state>
      </ng-template>
    </div>

    <div class="fab-container">
      <button mat-fab color="primary" (click)="openNewRule()">
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
})
export class RulesComponent implements OnInit {
  loading = true;
  rules: Rule[] = [];

  constructor(private dialog: MatDialog, private snackBar: MatSnackBar) {}

  ngOnInit() {
    setTimeout(() => {
      this.rules = RULES.map(r => ({ ...r }));
      this.loading = false;
    }, 600);
  }

  onToggleRule(rule: Rule) {
    this.snackBar.open(`"${rule.name}" ${rule.active ? 'enabled' : 'disabled'} ✓`, '', { duration: 2000 });
  }

  openNewRule() {
    const ref = this.dialog.open(NewRuleDialogComponent, { width: '560px' });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.rules = [...this.rules, {
          id: 'rl_' + Date.now(),
          name: result.name,
          summary: `When ${result.triggerLabel} → ${result.action}`,
          triggerType: result.triggerType,
          active: true,
        }];
        this.snackBar.open(`Rule "${result.name}" created ✓`, '', { duration: 2000 });
      }
    });
  }
}
