import { Room, Device, Scene, Schedule, ActivityEntry, EnergyDevice, EnergyRoom, Member } from './models';

export const ROOMS: Room[] = [
  { id: 'r1', name: 'Living Room', icon: 'weekend' },
  { id: 'r2', name: 'Kitchen', icon: 'kitchen' },
  { id: 'r3', name: 'Bedroom', icon: 'bed' },
  { id: 'r4', name: 'Hallway', icon: 'door_front' },
  { id: 'r5', name: 'Garage', icon: 'garage' },
];

export const DEVICES: Device[] = [
  // Living Room
  { id: 'd1', name: 'Ceiling Light', roomId: 'r1', type: 'dimmer', icon: 'lightbulb', state: { on: true, brightness: 70 } },
  { id: 'd2', name: 'TV', roomId: 'r1', type: 'switch', icon: 'tv', state: { on: true } },
  { id: 'd3', name: 'Smart Blinds', roomId: 'r1', type: 'cover', icon: 'blinds', state: { coverPosition: 60 } },
  { id: 'd4', name: 'Air Quality Sensor', roomId: 'r1', type: 'sensor', icon: 'air', state: { sensorValue: 42, sensorUnit: 'AQI' } },
  // Kitchen
  { id: 'd5', name: 'Counter Light', roomId: 'r2', type: 'switch', icon: 'lightbulb', state: { on: false } },
  { id: 'd6', name: 'Dishwasher', roomId: 'r2', type: 'switch', icon: 'dishwasher_gen', state: { on: false } },
  { id: 'd7', name: 'Temperature Sensor', roomId: 'r2', type: 'sensor', icon: 'thermostat', state: { sensorValue: 22.5, sensorUnit: '°C' } },
  // Bedroom
  { id: 'd8', name: 'Bedside Lamp', roomId: 'r3', type: 'dimmer', icon: 'lightbulb', state: { on: false, brightness: 30 } },
  { id: 'd9', name: 'AC Unit', roomId: 'r3', type: 'thermostat', icon: 'ac_unit', state: { on: true, temperature: 21 } },
  // Hallway
  { id: 'd10', name: 'Motion Sensor', roomId: 'r4', type: 'sensor', icon: 'sensors', state: { sensorValue: 0, sensorUnit: 'motion' } },
  { id: 'd11', name: 'Hallway Light', roomId: 'r4', type: 'switch', icon: 'lightbulb', state: { on: false } },
  // Garage
  { id: 'd12', name: 'Garage Door', roomId: 'r5', type: 'cover', icon: 'garage', state: { coverPosition: 0 } },
];

export const SCENES: Scene[] = [
  {
    id: 's1', name: 'Morning Routine', icon: 'wb_sunny', description: 'Opens blinds · Dims lights to 50% · Sets AC to 22°C',
    deviceActions: [{ deviceId: 'd3', action: 'open' }, { deviceId: 'd1', action: 'dim-50' }, { deviceId: 'd9', action: 'set-22' }]
  },
  {
    id: 's2', name: 'Movie Night', icon: 'movie', description: 'Dims lights to 20% · Closes blinds · Turns on TV',
    deviceActions: [{ deviceId: 'd1', action: 'dim-20' }, { deviceId: 'd3', action: 'close' }, { deviceId: 'd2', action: 'on' }]
  },
  {
    id: 's3', name: 'Goodnight', icon: 'bedtime', description: 'Turns off all lights · Closes blinds · Locks garage',
    deviceActions: [{ deviceId: 'd1', action: 'off' }, { deviceId: 'd5', action: 'off' }, { deviceId: 'd12', action: 'close' }]
  },
  {
    id: 's4', name: 'Away Mode', icon: 'flight_takeoff', description: 'Turns off all devices · Closes covers · Activates sensors',
    deviceActions: [{ deviceId: 'd2', action: 'off' }, { deviceId: 'd3', action: 'close' }, { deviceId: 'd12', action: 'close' }]
  },
  {
    id: 's5', name: 'Party Mode', icon: 'celebration', description: 'Full brightness · Opens blinds · Turns on TV',
    deviceActions: [{ deviceId: 'd1', action: 'dim-100' }, { deviceId: 'd3', action: 'open' }, { deviceId: 'd2', action: 'on' }]
  },
];


export const SCHEDULES: Schedule[] = [
  { id: 'sc1', name: 'Morning Lights', deviceId: 'd1', deviceName: 'Ceiling Light', action: 'Turn On 60%', days: [0,1,2,3,4], startTime: '07:00', color: '#00897B', recurrence: 'weekdays' },
  { id: 'sc2', name: 'TV Off', deviceId: 'd2', deviceName: 'TV', action: 'Turn Off', days: [0,1,2,3,4,5,6], startTime: '23:00', color: '#5C6BC0', recurrence: 'daily' },
  { id: 'sc3', name: 'Garage Door Open', deviceId: 'd12', deviceName: 'Garage Door', action: 'Open', days: [0,1,2,3,4], startTime: '08:30', color: '#F4511E', recurrence: 'weekdays' },
  { id: 'sc4', name: 'Weekend Dim', deviceId: 'd1', deviceName: 'Ceiling Light', action: 'Dim to 30%', days: [5,6], startTime: '09:00', color: '#E67C00', recurrence: 'weekends' },
  { id: 'sc5', name: 'Bedside Lamp', deviceId: 'd8', deviceName: 'Bedside Lamp', action: 'Turn On 20%', days: [0,1,2,3,4,5,6], startTime: '20:00', color: '#8E24AA', recurrence: 'daily' },
];

const now = new Date();
const minsAgo = (m: number) => new Date(now.getTime() - m * 60000);
const hoursAgo = (h: number) => new Date(now.getTime() - h * 3600000);

export const ACTIVITY_LOG: ActivityEntry[] = [
  { id: 'a1', timestamp: minsAgo(2), deviceName: 'Ceiling Light', room: 'Living Room', description: 'Ceiling Light turned on by you', triggeredBy: 'Alex', deviceType: 'dimmer' },
  { id: 'a2', timestamp: minsAgo(8), deviceName: 'TV', room: 'Living Room', description: 'TV turned on by you', triggeredBy: 'Alex', deviceType: 'switch' },
  { id: 'a3', timestamp: minsAgo(15), deviceName: 'Hallway Light', room: 'Hallway', description: 'Hallway Light turned on by Motion Lighting rule', triggeredBy: 'Rule', deviceType: 'switch' },
  { id: 'a4', timestamp: minsAgo(23), deviceName: 'Motion Sensor', room: 'Hallway', description: 'Motion detected in Hallway', triggeredBy: 'Sensor', deviceType: 'sensor' },
  { id: 'a5', timestamp: minsAgo(47), deviceName: 'Smart Blinds', room: 'Living Room', description: 'Smart Blinds opened by Morning Wake-Up rule', triggeredBy: 'Rule', deviceType: 'cover' },
  { id: 'a6', timestamp: hoursAgo(1), deviceName: 'AC Unit', room: 'Bedroom', description: 'AC Unit set to 21°C by Sarah', triggeredBy: 'Sarah', deviceType: 'thermostat' },
  { id: 'a7', timestamp: hoursAgo(2), deviceName: 'Ceiling Light', room: 'Living Room', description: 'Morning Routine scene activated by Alex', triggeredBy: 'Alex', deviceType: 'dimmer' },
  { id: 'a8', timestamp: hoursAgo(3), deviceName: 'Garage Door', room: 'Garage', description: 'Garage Door opened by schedule', triggeredBy: 'Schedule', deviceType: 'cover' },
  { id: 'a9', timestamp: hoursAgo(4), deviceName: 'Counter Light', room: 'Kitchen', description: 'Counter Light turned off by you', triggeredBy: 'Alex', deviceType: 'switch' },
  { id: 'a10', timestamp: hoursAgo(5), deviceName: 'Bedside Lamp', room: 'Bedroom', description: 'Bedside Lamp turned on at 20% by schedule', triggeredBy: 'Schedule', deviceType: 'dimmer' },
  { id: 'a11', timestamp: hoursAgo(6), deviceName: 'Temperature Sensor', room: 'Kitchen', description: 'Temperature threshold alert: 29°C detected', triggeredBy: 'Sensor', deviceType: 'sensor' },
  { id: 'a12', timestamp: hoursAgo(7), deviceName: 'Dishwasher', room: 'Kitchen', description: 'Dishwasher turned on by Sarah', triggeredBy: 'Sarah', deviceType: 'switch' },
  { id: 'a13', timestamp: hoursAgo(8), deviceName: 'TV', room: 'Living Room', description: 'TV turned off by Night Mode rule', triggeredBy: 'Rule', deviceType: 'switch' },
  { id: 'a14', timestamp: hoursAgo(9), deviceName: 'Smart Blinds', room: 'Living Room', description: 'Smart Blinds closed by Night Mode rule', triggeredBy: 'Rule', deviceType: 'cover' },
  { id: 'a15', timestamp: hoursAgo(10), deviceName: 'Hallway Light', room: 'Hallway', description: 'Hallway Light turned off automatically after 5 minutes', triggeredBy: 'Rule', deviceType: 'switch' },
  { id: 'a16', timestamp: hoursAgo(12), deviceName: 'AC Unit', room: 'Bedroom', description: 'AC Unit turned off by Goodnight scene', triggeredBy: 'Alex', deviceType: 'thermostat' },
  { id: 'a17', timestamp: hoursAgo(14), deviceName: 'Ceiling Light', room: 'Living Room', description: 'Brightness adjusted to 80% by you', triggeredBy: 'Alex', deviceType: 'dimmer' },
  { id: 'a18', timestamp: hoursAgo(18), deviceName: 'Air Quality Sensor', room: 'Living Room', description: 'Air quality reading: 42 AQI (Good)', triggeredBy: 'Sensor', deviceType: 'sensor' },
  { id: 'a19', timestamp: hoursAgo(22), deviceName: 'Garage Door', room: 'Garage', description: 'Garage Door closed by Away Mode scene', triggeredBy: 'Sarah', deviceType: 'cover' },
  { id: 'a20', timestamp: hoursAgo(24), deviceName: 'Bedside Lamp', room: 'Bedroom', description: 'Bedside Lamp turned off by Goodnight scene', triggeredBy: 'Alex', deviceType: 'dimmer' },
];

export const ENERGY_DEVICES: EnergyDevice[] = [
  { deviceId: 'd1', deviceName: 'Ceiling Light', room: 'Living Room', wattage: 12, todayKwh: 0.18 },
  { deviceId: 'd2', deviceName: 'TV', room: 'Living Room', wattage: 85, todayKwh: 0.51 },
  { deviceId: 'd5', deviceName: 'Counter Light', room: 'Kitchen', wattage: 8, todayKwh: 0.04 },
  { deviceId: 'd6', deviceName: 'Dishwasher', room: 'Kitchen', wattage: 1200, todayKwh: 0.60 },
  { deviceId: 'd8', deviceName: 'Bedside Lamp', room: 'Bedroom', wattage: 6, todayKwh: 0.06 },
  { deviceId: 'd9', deviceName: 'AC Unit', room: 'Bedroom', wattage: 900, todayKwh: 0.27 },
  { deviceId: 'd11', deviceName: 'Hallway Light', room: 'Hallway', wattage: 5, todayKwh: 0.02 },
  { deviceId: 'd12', deviceName: 'Garage Door', room: 'Garage', wattage: 350, todayKwh: 0.12 },
];

export const ENERGY_ROOMS: EnergyRoom[] = [
  { roomName: 'Living Room', todayKwh: 0.69, weekKwh: 4.8 },
  { roomName: 'Kitchen', todayKwh: 0.64, weekKwh: 4.5 },
  { roomName: 'Bedroom', todayKwh: 0.33, weekKwh: 2.3 },
  { roomName: 'Hallway', todayKwh: 0.02, weekKwh: 0.14 },
  { roomName: 'Garage', todayKwh: 0.12, weekKwh: 0.66 },
];

export const MEMBERS: Member[] = [
  { id: 'm1', name: 'Alex Johnson', email: 'alex@example.com', role: 'Owner', avatarInitials: 'AJ' },
  { id: 'm2', name: 'Sarah Miller', email: 'sarah@example.com', role: 'Member', avatarInitials: 'SM' },
  { id: 'm3', name: 'Tom Davis', email: 'tom@example.com', role: 'Member', avatarInitials: 'TD' },
];
