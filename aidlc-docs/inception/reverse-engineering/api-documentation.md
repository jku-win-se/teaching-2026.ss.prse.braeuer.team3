# API Documentation

## REST APIs

### Auth

#### POST /api/auth/register
- **Purpose**: Neues Benutzerkonto anlegen
- **Request**: `{ "name": "string", "email": "string", "password": "string" }`
- **Response**: `{ "token": "string", "name": "string", "email": "string" }` (201)

#### POST /api/auth/login
- **Purpose**: Einloggen, JWT erhalten
- **Request**: `{ "email": "string", "password": "string" }`
- **Response**: `{ "token": "string", "name": "string", "email": "string" }` (200)

---

### Rooms (Auth required)

#### GET /api/rooms
- **Purpose**: Alle Räume des eingeloggten Benutzers abrufen
- **Response**: `List<RoomResponse>` (200)

#### POST /api/rooms
- **Purpose**: Neuen Raum anlegen
- **Request**: `{ "name": "string", "icon": "string" }`
- **Response**: `RoomResponse` (201)

#### PUT /api/rooms/{id}
- **Purpose**: Raum umbenennen
- **Request**: `{ "name": "string", "icon": "string" }`
- **Response**: `RoomResponse` (200)

#### DELETE /api/rooms/{id}
- **Purpose**: Raum löschen (kaskadiert Geräte)
- **Response**: 204

---

### Devices (Auth required, scoped to room)

#### GET /api/rooms/{roomId}/devices
- **Purpose**: Alle Geräte eines Raums abrufen
- **Response**: `List<DeviceResponse>` (200)

#### POST /api/rooms/{roomId}/devices
- **Purpose**: Neues Gerät anlegen
- **Request**: `{ "name": "string", "type": "SWITCH|DIMMER|THERMOSTAT|SENSOR|COVER" }`
- **Response**: `DeviceResponse` (201)

#### PUT /api/rooms/{roomId}/devices/{deviceId}
- **Purpose**: Gerät umbenennen
- **Request**: `{ "name": "string" }`
- **Response**: `DeviceResponse` (200)

#### PATCH /api/rooms/{roomId}/devices/{deviceId}/state
- **Purpose**: Gerätezustand partiell aktualisieren
- **Request**: `DeviceStateRequest` — alle Felder optional: `stateOn`, `brightness`, `temperature`, `sensorValue`, `coverPosition`
- **Response**: `DeviceResponse` (200)

#### DELETE /api/rooms/{roomId}/devices/{deviceId}
- **Purpose**: Gerät löschen
- **Response**: 204

---

## Data Models

### DeviceResponse
| Feld | Typ | Beschreibung |
|------|-----|--------------|
| id | String | UUID/ID des Geräts |
| name | String | Anzeigename |
| type | String | SWITCH / DIMMER / THERMOSTAT / SENSOR / COVER |
| stateOn | Boolean | Ein/Aus-Zustand |
| brightness | Integer | 0–100 (DIMMER) |
| temperature | Double | Grad (THERMOSTAT) |
| sensorValue | Double | Messwert (SENSOR) |
| coverPosition | Integer | 0=geschlossen, 100=offen (COVER) |

### DeviceStateRequest
| Feld | Typ | Pflicht |
|------|-----|---------|
| stateOn | Boolean | nein |
| brightness | Integer | nein |
| temperature | Double | nein |
| sensorValue | Double | nein |
| coverPosition | Integer | nein |
