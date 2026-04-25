import { Device, Room } from './models';
import { DeviceDto } from './device.service';
import { RoomDto } from './room.service';

export const DEVICE_ICON: Record<string, string> = {
  switch: 'lightbulb', dimmer: 'lightbulb',
  thermostat: 'thermostat', sensor: 'sensors', cover: 'blinds',
};

export const DEVICE_ICON_BG: Record<string, string> = {
  switch: 'rgba(249,115,22,0.1)', dimmer: 'rgba(249,115,22,0.1)',
  thermostat: 'rgba(139,92,246,0.1)', sensor: 'rgba(79,70,229,0.1)', cover: 'rgba(16,185,129,0.1)',
};

export const DEVICE_ICON_COLOR: Record<string, string> = {
  switch: '#F97316', dimmer: '#F97316',
  thermostat: '#8B5CF6', sensor: '#4F46E5', cover: '#10B981',
};

export function toRoom(dto: RoomDto): Room {
  return { id: String(dto.id), name: dto.name, icon: dto.icon };
}

export function dtoToDevice(dto: DeviceDto, roomId: string): Device {
  return {
    id: String(dto.id),
    name: dto.name,
    roomId,
    type: dto.type,
    icon: DEVICE_ICON[dto.type] ?? 'devices',
    state: {
      on: dto.stateOn,
      brightness: dto.brightness,
      temperature: dto.temperature,
      sensorValue: dto.sensorValue,
      sensorUnit: '°C',
      coverPosition: dto.coverPosition,
    },
  };
}
