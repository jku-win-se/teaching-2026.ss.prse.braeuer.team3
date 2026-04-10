import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { SCENES } from '../../core/mock-data';
import { Scene } from '../../core/models';
import { NewSceneDialogComponent } from './new-scene-dialog.component';

@Component({
  selector: 'app-scenes',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule,
    MatProgressBarModule, MatDialogModule, MatSnackBarModule,
  ],
  template: `
    <div *ngIf="loading"><mat-progress-bar mode="indeterminate"></mat-progress-bar></div>
    <div class="page-container" *ngIf="!loading">
      <div class="page-header">
        <h1>Scenes</h1>
        <p class="subtitle">Activate multiple devices at once with a single tap.</p>
      </div>

      <div class="scenes-grid">
        <mat-card *ngFor="let scene of scenes" [class.scene-active]="activeSceneId === scene.id">
          <div class="scene-card-content" [class.scene-active]="activeSceneId === scene.id">
            <mat-icon class="scene-big-icon">{{ scene.icon }}</mat-icon>
            <h3>{{ scene.name }}</h3>
            <p class="scene-desc">{{ scene.description }}</p>
            <div style="display:flex;gap:8px;justify-content:center;">
              <button mat-flat-button color="primary" (click)="activateScene(scene)">
                <mat-icon>play_arrow</mat-icon> Activate
              </button>
              <button mat-icon-button (click)="editScene(scene)" title="Edit scene">
                <mat-icon>edit</mat-icon>
              </button>
            </div>
          </div>
        </mat-card>
      </div>
    </div>

    <div class="fab-container">
      <button mat-fab color="primary" (click)="openNewScene()">
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
})
export class ScenesComponent implements OnInit {
  loading = true;
  scenes: Scene[] = [];
  activeSceneId: string | null = null;

  constructor(private dialog: MatDialog, private snackBar: MatSnackBar) {}

  ngOnInit() {
    setTimeout(() => {
      this.scenes = SCENES.map(s => ({ ...s }));
      this.loading = false;
    }, 600);
  }

  activateScene(scene: Scene) {
    this.activeSceneId = scene.id;
    this.snackBar.open(`${scene.name} activated ✓`, '', { duration: 2000 });
    setTimeout(() => { if (this.activeSceneId === scene.id) this.activeSceneId = null; }, 2000);
  }

  editScene(scene: Scene) {
    const ref = this.dialog.open(NewSceneDialogComponent, {
      width: '520px',
      data: { scene }
    });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.scenes = this.scenes.map(s => s.id === scene.id ? { ...s, ...result } : s);
        this.snackBar.open(`Scene "${result.name}" updated ✓`, '', { duration: 2000 });
      }
    });
  }

  openNewScene() {
    const ref = this.dialog.open(NewSceneDialogComponent, {
      width: '520px',
      data: { scene: null }
    });
    ref.afterClosed().subscribe(result => {
      if (result) {
        this.scenes = [...this.scenes, {
          id: 's_' + Date.now(),
          name: result.name,
          icon: result.icon,
          description: result.description,
          deviceActions: [],
        }];
        this.snackBar.open(`Scene "${result.name}" created ✓`, '', { duration: 2000 });
      }
    });
  }
}
