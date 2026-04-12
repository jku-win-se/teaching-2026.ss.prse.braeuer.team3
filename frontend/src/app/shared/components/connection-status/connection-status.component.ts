import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ConnectionState } from '../../../core/realtime.service';

/**
 * Displays a warning banner when the SSE real-time connection is not active (FR-07 AC-1).
 *
 * <p>Hidden when {@code state} is {@code 'connected'}. Shows a reconnecting or
 * connection-lost message otherwise.</p>
 */
@Component({
  selector: 'app-connection-status',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div *ngIf="state !== 'connected'" class="connection-banner" [class.reconnecting]="state === 'reconnecting'">
      <mat-icon style="font-size:18px;width:18px;height:18px;vertical-align:middle;margin-right:6px;">
        {{ state === 'reconnecting' ? 'sync' : 'wifi_off' }}
      </mat-icon>
      <span>{{ state === 'reconnecting' ? 'Reconnecting to real-time updates…' : 'Connection lost. Real-time updates paused.' }}</span>
    </div>
  `,
  styles: [`
    .connection-banner {
      display: flex;
      align-items: center;
      padding: 8px 16px;
      background: #fff3e0;
      color: #e65100;
      border-radius: 4px;
      margin-bottom: 12px;
      font-size: 13px;
    }
    .connection-banner.reconnecting {
      background: #fff8e1;
      color: #f57f17;
    }
  `],
})
export class ConnectionStatusComponent {
  /** Current SSE connection state — controls banner visibility and message. */
  @Input() state: ConnectionState = 'connected';
}
