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

export type TriggerType = 'TIME' | 'THRESHOLD' | 'EVENT';

/** Shape of a rule as returned by the backend API (US-012). */
export interface RuleDto {
  id: number;
  name: string;
  enabled: boolean;
  triggerType: TriggerType;
  triggerDeviceId?: number;
  triggerDeviceName?: string;
  triggerOperator?: 'GT' | 'LT';
  triggerThresholdValue?: number;
  triggerHour?: number;
  triggerMinute?: number;
  triggerDaysOfWeek?: string;
  actionDeviceId: number;
  actionDeviceName: string;
  actionValue: string;
}

/** Request body for creating or updating a rule (US-012). */
export interface RuleRequest {
  name: string;
  enabled: boolean;
  triggerType: TriggerType;
  triggerDeviceId?: number;
  triggerOperator?: 'GT' | 'LT';
  triggerThresholdValue?: number;
  triggerHour?: number;
  triggerMinute?: number;
  triggerDaysOfWeek?: string;
  actionDeviceId: number;
  actionValue: string;
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

/** Day-of-week definitions shared between schedule dialog and schedule list. */
export const DAYS = [
  { label: 'Mon', value: 'MONDAY' },
  { label: 'Tue', value: 'TUESDAY' },
  { label: 'Wed', value: 'WEDNESDAY' },
  { label: 'Thu', value: 'THURSDAY' },
  { label: 'Fri', value: 'FRIDAY' },
  { label: 'Sat', value: 'SATURDAY' },
  { label: 'Sun', value: 'SUNDAY' },
];

/** Shape of a schedule as returned by the backend API (FR-09). */
export interface ScheduleDto {
  id: number;
  name: string;
  deviceId: number;
  deviceName: string;
  roomName: string;
  daysOfWeek: string[];   // uppercase Java DayOfWeek names, e.g. ["MONDAY","FRIDAY"]
  hour: number;           // 0–23
  minute: number;         // 0–59
  actionPayload: string;  // JSON string e.g. {"stateOn":true}
  enabled: boolean;
}

/** Request body for creating or updating a schedule (FR-09). */
export interface ScheduleRequest {
  name: string;
  deviceId: number;
  daysOfWeek: string[];
  hour: number;
  minute: number;
  actionPayload: string;
  enabled: boolean;
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

/** Shape of an activity log entry as returned by the backend API (FR-08). */
export interface ActivityLogDto {
  id: number;
  timestamp: string;  // ISO-8601 string from backend
  deviceId: number;
  deviceName: string;
  roomName: string;
  actorName: string;
  action: string;
  messageType?: string; // present when received via WebSocket
}

/** Shape of a paginated Spring Data Page as returned by the backend. */
export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;  // current page index
  size: number;
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
