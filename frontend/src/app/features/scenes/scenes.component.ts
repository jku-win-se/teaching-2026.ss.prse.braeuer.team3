import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SceneService } from '../../core/scene.service';
import { SceneDto } from '../../core/models';
import { NewSceneDialogComponent } from './new-scene-dialog.component';

/**
 * Page component for scene management.
 *
 * Displays all scenes owned by the authenticated user and provides
 * controls to create, activate, edit, and delete them (US-018).
 */
@Component({
  selector: 'app-scenes',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatDialogModule, MatSnackBarModule, MatTooltipModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>

    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Scenes</h1>
        <p class="subtitle">Activate multiple devices at once with a single tap.</p>
      </div>

      <div *ngIf="scenes.length === 0"
           style="display:flex;flex-direction:column;align-items:center;gap:12px;padding:48px 16px;color:#757575;">
        <mat-icon style="font-size:48px;width:48px;height:48px;">auto_awesome</mat-icon>
        <p style="margin:0;font-size:15px;text-align:center;">
          No scenes yet.<br>Create your first scene with the + button below.
        </p>
      </div>

      <div class="scenes-grid">
        <mat-card *ngFor="let scene of scenes" [class.scene-active]="activeSceneId === scene.id">
          <div class="scene-card-content">
            <mat-icon class="scene-big-icon">{{ scene.icon }}</mat-icon>
            <h3>{{ scene.name }}</h3>
            <p class="scene-desc" *ngIf="scene.entries.length > 0">
              {{ describeEntries(scene) }}
            </p>
            <div style="display:flex;gap:8px;justify-content:center;flex-wrap:wrap;">
              <button
                mat-flat-button
                color="primary"
                (click)="activateScene(scene)"
                [disabled]="activatingId === scene.id"
                data-testid="scene-activate-button">
                <mat-icon>play_arrow</mat-icon>
                {{ activatingId === scene.id ? 'Activating…' : 'Activate' }}
              </button>
              <button mat-icon-button (click)="editScene(scene)"
                      matTooltip="Edit scene" data-testid="scene-edit-button">
                <mat-icon>edit</mat-icon>
              </button>
              <button mat-icon-button color="warn" (click)="deleteScene(scene)"
                      matTooltip="Delete scene" data-testid="scene-delete-button">
                <mat-icon>delete</mat-icon>
              </button>
            </div>
          </div>
        </mat-card>
      </div>
    </div>

    <div class="fab-container">
      <button mat-fab color="primary" (click)="openNewScene()" data-testid="scene-create-fab">
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
})
export class ScenesComponent implements OnInit {
  loading = true;
  scenes: SceneDto[] = [];
  activeSceneId: number | null = null;
  activatingId: number | null = null;

  constructor(
    private sceneService: SceneService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
  ) {}

  /** Loads all scenes from the backend on component init. */
  ngOnInit(): void {
    this.loadScenes();
  }

  /** Activates a scene by calling the backend activate endpoint. */
  activateScene(scene: SceneDto): void {
    this.activatingId = scene.id;
    this.activeSceneId = scene.id;
    this.sceneService.activateScene(scene.id).subscribe({
      next: () => {
        this.snackBar.open(`${scene.name} activated ✓`, '', { duration: 2500 });
        this.activatingId = null;
        setTimeout(() => {
          if (this.activeSceneId === scene.id) {
            this.activeSceneId = null;
          }
        }, 2500);
      },
      error: () => {
        this.snackBar.open('Failed to activate scene.', 'Dismiss', { duration: 3000 });
        this.activatingId = null;
        this.activeSceneId = null;
      },
    });
  }

  /** Opens the edit dialog for an existing scene. */
  editScene(scene: SceneDto): void {
    const ref = this.dialog.open(NewSceneDialogComponent, {
      width: '560px',
      data: { scene },
    });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.sceneService.updateScene(scene.id, result).subscribe({
          next: updated => {
            this.scenes = this.scenes.map(s => s.id === scene.id ? updated : s);
            this.snackBar.open(`Scene "${updated.name}" updated ✓`, '', { duration: 2000 });
          },
          error: () => this.snackBar.open('Failed to update scene.', 'Dismiss', { duration: 3000 }),
        });
      }
    });
  }

  /** Deletes a scene immediately after the user clicks delete. */
  deleteScene(scene: SceneDto): void {
    this.sceneService.deleteScene(scene.id).subscribe({
      next: () => {
        this.scenes = this.scenes.filter(s => s.id !== scene.id);
        this.snackBar.open(`Scene "${scene.name}" deleted.`, '', { duration: 2000 });
      },
      error: () => this.snackBar.open('Failed to delete scene.', 'Dismiss', { duration: 3000 }),
    });
  }

  /** Opens the create dialog for a new scene. */
  openNewScene(): void {
    const ref = this.dialog.open(NewSceneDialogComponent, {
      width: '560px',
      data: { scene: null },
    });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.sceneService.createScene(result).subscribe({
          next: created => {
            this.scenes = [...this.scenes, created];
            this.snackBar.open(`Scene "${created.name}" created ✓`, '', { duration: 2000 });
          },
          error: () => this.snackBar.open('Failed to create scene.', 'Dismiss', { duration: 3000 }),
        });
      }
    });
  }

  /** Returns a short human-readable summary of the device actions in a scene. */
  describeEntries(scene: SceneDto): string {
    return scene.entries
      .map(e => `${e.deviceName}: ${this.actionLabel(e.actionValue)}`)
      .join(' · ');
  }

  private actionLabel(value: string): string {
    switch (value) {
      case 'true':  return 'On';
      case 'false': return 'Off';
      case 'open':  return 'Open';
      case 'close': return 'Close';
      default:      return value;
    }
  }

  private loadScenes(): void {
    this.loading = true;
    this.sceneService.getScenes().subscribe({
      next: scenes => {
        this.scenes = scenes;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Failed to load scenes.', 'Dismiss', { duration: 3000 });
      },
    });
  }
}
