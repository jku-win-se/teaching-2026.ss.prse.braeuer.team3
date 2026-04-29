# Story Map — US-012

| Story / Anforderung                                             | Unit   | Akzeptanzkriterium                                       |
|-----------------------------------------------------------------|--------|----------------------------------------------------------|
| Zeitbasierter Trigger (Uhrzeit + Wochentage) konfigurierbar     | Unit 1 + Unit 2 | Regel mit TIME persistiert, Scheduler führt sie aus |
| Schwellenwertbasierter Trigger (Sensor GT/LT) konfigurierbar    | Unit 2 | Bestehende THRESHOLD-Logik via echte API nutzbar         |
| Ereignisbasierter Trigger (Device on/off) konfigurierbar        | Unit 2 | Bestehende EVENT-Logik via echte API nutzbar             |
| Regelliste lädt echte Daten                                     | Unit 2 | Kein RULES-Mock-Import in RulesComponent                 |
| Neue Regel wird gespeichert                                     | Unit 2 | POST /api/rules, erscheint in Listenansicht              |
| Toggle Enable/Disable persistiert                               | Unit 2 | PATCH /api/rules/{id}/enabled                            |
| Edit einer bestehenden Regel                                    | Unit 2 | PUT /api/rules/{id}, Dialog vorausgefüllt                |
| Delete einer Regel mit Bestätigungsdialog                       | Unit 2 | DELETE /api/rules/{id}                                   |
| Geräteauswahl Raum → Gerät (kein Mock)                          | Unit 2 | Dialog lädt Rooms + Devices via API                      |
| Operatoren auf GT und LT beschränkt                             | Unit 2 | Nur `>` und `<` im Threshold-Dialog                     |
