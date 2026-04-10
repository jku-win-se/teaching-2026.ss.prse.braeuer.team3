import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DeviceType } from './models';

/** Shape of a device as returned by the backend API. */
export interface DeviceDto {
  id: number;
  name: string;
  type: DeviceType; // lowercase — matches frontend DeviceType union directly
  stateOn: boolean;
  brightness: number;
  temperature: number;
  sensorValue: number;
  coverPosition: number;
}

/** Shape of the add-device request body. */
export interface AddDeviceRequest {
  name: string;
  type: DeviceType; // sent as-is — backend @JsonCreator handles case-insensitive parsing
}

/**
 * HTTP client service for device management.
 * Covers FR-04: add virtual smart device to a room.
 * Covers FR-05: rename and remove devices.
 */
@Injectable({ providedIn: 'root' })
export class DeviceService {
  private readonly BASE = 'http://localhost:8080/api/rooms';

  constructor(private http: HttpClient) {}

  /**
   * Returns all devices in the given room.
   *
   * @param roomId the room's primary key
   * @returns observable list of devices
   */
  getDevices(roomId: number): Observable<DeviceDto[]> {
    return this.http.get<DeviceDto[]>(`${this.BASE}/${roomId}/devices`);
  }

  /**
   * Adds a new virtual device to the given room.
   * FR-04: Gerät hinzufügen.
   *
   * @param roomId the room's primary key
   * @param req    the device creation request
   * @returns observable of the newly created device
   */
  addDevice(roomId: number, req: AddDeviceRequest): Observable<DeviceDto> {
    return this.http.post<DeviceDto>(`${this.BASE}/${roomId}/devices`, req);
  }

  /**
   * Renames an existing device.
   * FR-05: Gerät umbenennen.
   *
   * @param roomId   the room's primary key
   * @param deviceId the device's primary key
   * @param name     the new device name
   * @returns observable of the updated device
   */
  renameDevice(roomId: number, deviceId: number, name: string): Observable<DeviceDto> {
    return this.http.put<DeviceDto>(`${this.BASE}/${roomId}/devices/${deviceId}`, { name });
  }

  /**
   * Deletes a device from a room.
   * FR-05: Gerät löschen.
   *
   * @param roomId   the room's primary key
   * @param deviceId the device's primary key
   * @returns observable that completes when the device is deleted
   */
  removeDevice(roomId: number, deviceId: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${roomId}/devices/${deviceId}`);
  }

  /**
   * Partially updates the runtime state of a device.
   * FR-06: Gerät manuell steuern.
   *
   * @param roomId   the room's primary key
   * @param deviceId the device's primary key
   * @param state    partial state object — only provided fields are applied
   * @returns observable of the updated device with its new state
   */
  updateState(roomId: number, deviceId: number, state: object): Observable<DeviceDto> {
    return this.http.patch<DeviceDto>(`${this.BASE}/${roomId}/devices/${deviceId}/state`, state);
  }
}
