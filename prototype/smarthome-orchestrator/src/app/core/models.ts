export type DeviceType = 'switch' | 'dimmer' | 'thermostat' | 'sensor' | 'cover';

export interface DeviceState {
  on?: boolean;
  brightness?: number;       // 0-100 for dimmer
  temperature?: number;      // degrees for thermostat
  sensorValue?: number;      // current reading for sensor
  sensorUnit?: string;       // e.g. '°C', '%', 'lux'
  coverPosition?: number;    // 0=closed, 100=open
}

export interface Device {
  id: string;
  name: string;
  roomId: string;
  type: DeviceType;
  state: DeviceState;
  icon: string;
}

export interface Room {
  id: string;
  name: string;
  icon: string;
}

export interface Scene {
  id: string;
  name: string;
  icon: string;
  description: string;
  deviceActions: { deviceId: string; action: string }[];
}

export type TriggerType = 'time' | 'threshold' | 'event';

export interface Rule {
  id: string;
  name: string;
  summary: string;
  triggerType: TriggerType;
  active: boolean;
  hasConflict?: boolean;
}

export type RecurrenceType = 'daily' | 'weekdays' | 'weekends' | 'custom';

export interface Schedule {
  id: string;
  name: string;
  deviceId: string;
  deviceName: string;
  action: string;
  days: number[];          // 0=Mon, 6=Sun
  startTime: string;       // HH:MM
  color: string;
  recurrence: RecurrenceType;
}

export interface ActivityEntry {
  id: string;
  timestamp: Date;
  deviceName: string;
  room: string;
  description: string;
  triggeredBy: string;
  deviceType: DeviceType;
}

export interface EnergyDevice {
  deviceId: string;
  deviceName: string;
  room: string;
  wattage: number;
  todayKwh: number;
}

export interface EnergyRoom {
  roomName: string;
  todayKwh: number;
  weekKwh: number;
}

export interface Member {
  id: string;
  name: string;
  email: string;
  role: 'Owner' | 'Member';
  avatarInitials: string;
}
