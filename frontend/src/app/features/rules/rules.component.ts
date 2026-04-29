import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { RuleDto } from '../../core/models';
import { RuleService } from '../../core/rule.service';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { NewRuleDialogComponent } from './new-rule-dialog.component';

@Component({
  selector: 'app-rules',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatSlideToggleModule,
    MatProgressBarModule, MatDialogModule, MatSnackBarModule, FormsModule,
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
              <mat-slide-toggle [(ngModel)]="rule.enabled" color="primary"
                                (change)="onToggleRule(rule)"
                                data-testid="rule-toggle">
              </mat-slide-toggle>
            </div>
            <p class="rule-summary">{{ buildSummary(rule) }}</p>
            <div class="rule-chips">
              <span class="trigger-chip">
                <span *ngIf="rule.triggerType === 'TIME'">🕐 Time</span>
                <span *ngIf="rule.triggerType === 'THRESHOLD'">📊 Threshold</span>
                <span *ngIf="rule.triggerType === 'EVENT'">⚡ Event</span>
              </span>
              <span *ngIf="!rule.enabled" style="font-size:12px;color:#9e9e9e;">Inactive</span>
            </div>
            <div class="rule-actions">
              <button mat-icon-button (click)="openEditRule(rule)" title="Edit" data-testid="rule-edit-btn">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="confirmDelete(rule)" title="Delete" data-testid="rule-delete-btn">
                <mat-icon>delete</mat-icon>
              </button>
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
      <button mat-fab color="primary" (click)="openNewRule()" data-testid="rule-add-fab">
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .rule-actions { display: flex; justify-content: flex-end; margin-top: 4px; }
  `],
})
export class RulesComponent implements OnInit {
  loading = true;
  rules: RuleDto[] = [];

  constructor(
    private ruleService: RuleService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit() {
    this.loadRules();
  }

  loadRules() {
    this.loading = true;
    this.ruleService.getRules().subscribe({
      next: rules => { this.rules = rules; this.loading = false; },
      error: () => { this.loading = false; },
    });
  }

  onToggleRule(rule: RuleDto) {
    this.ruleService.setEnabled(rule.id, rule.enabled).subscribe({
      next: updated => {
        rule.enabled = updated.enabled;
        this.snackBar.open(`"${rule.name}" ${rule.enabled ? 'enabled' : 'disabled'} ✓`, '', { duration: 2000 });
      },
      error: () => {
        rule.enabled = !rule.enabled;
        this.snackBar.open('Failed to update rule', '', { duration: 2000 });
      },
    });
  }

  confirmDelete(rule: RuleDto) {
    const confirmed = confirm(`Delete rule "${rule.name}"?`);
    if (!confirmed) return;
    this.ruleService.deleteRule(rule.id).subscribe({
      next: () => {
        this.rules = this.rules.filter(r => r.id !== rule.id);
        this.snackBar.open(`Rule "${rule.name}" deleted ✓`, '', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to delete rule', '', { duration: 2000 }),
    });
  }

  openNewRule() {
    const ref = this.dialog.open(NewRuleDialogComponent, { width: '580px', data: null });
    ref.afterClosed().subscribe(created => {
      if (created) this.loadRules();
    });
  }

  openEditRule(rule: RuleDto) {
    const ref = this.dialog.open(NewRuleDialogComponent, { width: '580px', data: rule });
    ref.afterClosed().subscribe(updated => {
      if (updated) this.loadRules();
    });
  }

  buildSummary(rule: RuleDto): string {
    let trigger = '';
    if (rule.triggerType === 'TIME') {
      const h = String(rule.triggerHour ?? 0).padStart(2, '0');
      const m = String(rule.triggerMinute ?? 0).padStart(2, '0');
      trigger = `Every ${rule.triggerDaysOfWeek ?? ''} at ${h}:${m}`;
    } else if (rule.triggerType === 'THRESHOLD') {
      const op = rule.triggerOperator === 'GT' ? '>' : '<';
      trigger = `${rule.triggerDeviceName} ${op} ${rule.triggerThresholdValue}`;
    } else {
      trigger = `${rule.triggerDeviceName} state changes`;
    }
    return `When ${trigger} → ${rule.actionValue} ${rule.actionDeviceName}`;
  }
}
