import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Persisted MQTT configuration returned by the backend (US-019). */
export interface MqttConfig {
  brokerUrl: string;
  topic: string;
  connected: boolean;
}

/** A single entry in the simulated MQTT message log. */
export interface MqttMessage {
  timestamp: string;
  direction: string;
  topic: string;
  payload: string;
}

/**
 * Angular service for the simulated MQTT integration (US-019).
 *
 * Wraps the {@code /api/mqtt} REST endpoints provided by the backend.
 * No real MQTT broker is contacted — all messaging is simulated in-memory
 * on the server.
 */
@Injectable({ providedIn: 'root' })
export class MqttService {

  private readonly base = 'http://localhost:8080/api/mqtt';

  constructor(private http: HttpClient) {}

  /** Returns the current MQTT configuration and connection status. */
  getConfig(): Observable<MqttConfig> {
    return this.http.get<MqttConfig>(`${this.base}/config`);
  }

  /** Saves the broker URL and topic, creating the config row if needed. */
  saveConfig(brokerUrl: string, topic: string): Observable<MqttConfig> {
    return this.http.put<MqttConfig>(`${this.base}/config`, { brokerUrl, topic });
  }

  /** Simulates establishing the MQTT connection. */
  connect(): Observable<MqttConfig> {
    return this.http.post<MqttConfig>(`${this.base}/connect`, {});
  }

  /** Simulates disconnecting from the MQTT broker. */
  disconnect(): Observable<MqttConfig> {
    return this.http.post<MqttConfig>(`${this.base}/disconnect`, {});
  }

  /** Returns the rolling in-memory message log (oldest first). */
  getMessages(): Observable<MqttMessage[]> {
    return this.http.get<MqttMessage[]>(`${this.base}/messages`);
  }

  /** Clears the in-memory message log. */
  clearMessages(): Observable<void> {
    return this.http.delete<void>(`${this.base}/messages`);
  }
}
