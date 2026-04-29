# Unit of Work Dependencies — US-012

| Von        | Nach       | Typ     | Grund                                              |
|------------|------------|---------|----------------------------------------------------|
| Unit 2     | Unit 1     | Compile | `RuleDto` / `RuleRequest` müssen TIME-Felder haben |

## Externe Abhängigkeiten

| Unit   | Abhängigkeit         | Typ     | Zweck                                       |
|--------|----------------------|---------|---------------------------------------------|
| Unit 1 | FR-09 ScheduleService | Pattern | `@Scheduled` + DayOfWeek-String-Format      |
| Unit 1 | DeviceService        | Runtime | `updateStateAsActor` beim Rule-Firing       |
| Unit 2 | RoomService          | Runtime | Räume laden für Gerät-Auswahl               |
| Unit 2 | DeviceService (FE)   | Runtime | Geräte je Raum laden                        |
