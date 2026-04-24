import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { DeviceDto } from './device.service';

/** WebSocket connection state. */
export type ConnectionState = 'connected' | 'disconnected' | 'reconnecting';

const WS_URL = 'ws://localhost:8080/ws/devices';
const RECONNECT_DELAY_MS = 5000;
const MAX_RECONNECT_ATTEMPTS = 10;

/**
 * Manages the WebSocket connection for real-time device state updates (FR-07).
 *
 * Opens a WebSocket connection to {@code /ws/devices?token=<JWT>}.
 * Emits parsed {@link DeviceDto} objects and tracks connection state for UI feedback.
 * Automatically reconnects on failure (up to {@code MAX_RECONNECT_ATTEMPTS} times).
 */
@Injectable({ providedIn: 'root' })
export class RealtimeService implements OnDestroy {

  private readonly connectionStateSubject = new BehaviorSubject<ConnectionState>('disconnected');
  /** Observable connection state — use in templates via the {@code async} pipe. */
  readonly state$ = this.connectionStateSubject.asObservable();

  private readonly deviceSubject = new Subject<DeviceDto>();
  /** Observable stream of device state updates received from the backend. */
  readonly deviceUpdates$ = this.deviceSubject.asObservable();

  private webSocket: WebSocket | null = null;
  private reconnectAttempts = 0;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  constructor(private readonly authService: AuthService) {}

  /**
   * Opens the WebSocket connection using the stored JWT as a query parameter.
   * Safe to call multiple times — closes any existing connection first.
   */
  connect(): void {
    this.disconnect();
    const token = this.authService.getToken();
    if (!token) {
      this.connectionStateSubject.next('disconnected');
      return;
    }
    const url = `${WS_URL}?token=${encodeURIComponent(token)}`;
    this.webSocket = new WebSocket(url);

    this.webSocket.onopen = () => {
      this.connectionStateSubject.next('connected');
      this.reconnectAttempts = 0;
    };

    this.webSocket.onmessage = (event: MessageEvent) => {
      try {
        const dto = JSON.parse(event.data as string) as DeviceDto;
        this.deviceSubject.next(dto);
      } catch {
        // Malformed message — ignore silently
      }
    };

    this.webSocket.onerror = () => {
      this.connectionStateSubject.next('reconnecting');
    };

    this.webSocket.onclose = () => {
      this.webSocket = null;
      if (this.connectionStateSubject.value !== 'disconnected') {
        this.scheduleReconnect();
      }
    };
  }

  /**
   * Closes the WebSocket connection and cancels any pending reconnect.
   */
  disconnect(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.webSocket !== null) {
      this.webSocket.onclose = null; // prevent reconnect loop
      this.webSocket.close();
      this.webSocket = null;
    }
    this.connectionStateSubject.next('disconnected');
  }

  /** @inheritDoc */
  ngOnDestroy(): void {
    this.disconnect();
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
      this.connectionStateSubject.next('disconnected');
      return;
    }
    this.reconnectAttempts++;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, RECONNECT_DELAY_MS);
  }
}
