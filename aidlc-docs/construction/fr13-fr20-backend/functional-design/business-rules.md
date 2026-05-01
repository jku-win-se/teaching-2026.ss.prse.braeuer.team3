# Business Rules — FR-13/FR-20 Backend

## BR-01: Rollenableitung

**Regel**: Die Rolle eines Users wird bei jeder Anfrage aus `home_members` gelesen, nicht aus dem JWT.

```
isMember(user) = home_members.findByMember(user).isPresent()
isOwner(user)  = !isMember(user)
```

**Konsequenz**: Ein entzogener Zugang (Delete aus `home_members`) wirkt beim nächsten API-Request sofort.

---

## BR-02: Einladungsvalidierung

**Regel**: Bevor eine Einladung gespeichert wird, müssen alle folgenden Bedingungen erfüllt sein:

| # | Bedingung | Fehler bei Verletzung |
|---|-----------|----------------------|
| 2a | Die eingeladene E-Mail ist registriert | `404 Not Found` — "User not found." |
| 2b | Der eingeladene User ist nicht identisch mit dem Owner | `400 Bad Request` — "Cannot invite yourself." |
| 2c | Der eingeladene User ist noch nicht Member in irgendeinem Home | `409 Conflict` — "User is already a member of a home." |
| 2d | Der eingeladene User hat selbst keine aktiven Mitglieder (d.h. er ist nicht Owner eines belegten Homes) | *(implizit durch exklusives Modell: ein Owner-User mit eigenem Home darf kein Member werden — aber im exklusiven Modell ist jeder User Owner, solange er kein Member ist; die Einladung macht ihn zum Member und seine eigenen Räume sind nicht mehr über die API erreichbar)* |

**Hinweis zu 2d**: Im exklusiven Modell sind eigene Räume eines Users nach der Einladung für ihn nicht mehr über die API zugänglich (weil alle Raum-Lookups auf den Owner umgeleitet werden). Die Räume bleiben in der DB erhalten.

---

## BR-03: Effektiver Eigentümerkontext (Owner Resolution)

**Regel**: Bei jeder authentifizierten Anfrage wird der **effektive Owner** für den DB-Lookup bestimmt:

```
resolveEffectiveOwner(callerEmail):
    caller = userRepository.findByEmail(callerEmail)
    membership = homeMemberRepository.findByMember(caller)
    if membership.isPresent():
        return membership.get().getOwner()   ← Member: Lookups gegen Owner-Daten
    else:
        return caller                         ← Owner: Lookups gegen eigene Daten
```

Alle Raum- und Gerätezugriffe (GET, PATCH /state) verwenden `resolveEffectiveOwner(callerEmail).getId()` als `user_id`-Filter.

---

## BR-04: OWNER-only Operationen (403 Enforcement)

**Regel**: Folgende Operationen sind ausschließlich für OWNER erlaubt. Ein Member erhält `403 Forbidden`.

| Endpunkt | Operation |
|----------|-----------|
| `POST /api/rooms` | Raum erstellen |
| `PUT /api/rooms/{id}` | Raum umbenennen |
| `DELETE /api/rooms/{id}` | Raum löschen |
| `POST /api/rooms/{id}/devices` | Gerät hinzufügen |
| `PUT /api/rooms/{id}/devices/{id}` | Gerät umbenennen |
| `DELETE /api/rooms/{id}/devices/{id}` | Gerät löschen |
| `POST /api/rules` | Regel erstellen |
| `PUT /api/rules/{id}` | Regel bearbeiten |
| `DELETE /api/rules/{id}` | Regel löschen |
| `POST /api/schedules` | Zeitplan erstellen |
| `PUT /api/schedules/{id}` | Zeitplan bearbeiten |
| `DELETE /api/schedules/{id}` | Zeitplan löschen |
| `POST /api/members/invite` | Member einladen (nur Owner) |
| `GET /api/members` | Mitgliederliste (nur Owner) |
| `DELETE /api/members/{id}` | Member entfernen (nur Owner) |
| `GET /api/activity-log` | Aktivitätsprotokoll anzeigen |
| `DELETE /api/activity-log/{id}` | Log-Eintrag löschen |

**Implementierung**: `requireOwnerRole(callerEmail)` wirft `403 Forbidden` falls Member.

---

## BR-05: Member-erlaubte Operationen

**Regel**: Folgende Operationen sind auch für Member erlaubt (mit Owner-Kontext):

| Endpunkt | Bemerkung |
|----------|-----------|
| `GET /api/rooms` | Gibt Räume des Owners zurück |
| `GET /api/rooms/{id}/devices` | Gibt Geräte des Owner-Raums zurück |
| `PATCH /api/rooms/{id}/devices/{id}/state` | Gerätezustand ändern (Member-Name im Log) |

---

## BR-06: Activity Log bei Member-Aktionen

**Regel**: Wenn ein Member einen Gerätezustand ändert:

```
activityLogService.log(
    device    = das geänderte Gerät,
    user      = effectiveOwner,        ← für Scoping (Owner-Kontext)
    actorName = member.getName(),      ← Member erscheint als Akteur
    action    = buildActionDescription(...)
)
webSocketHandler.broadcast(effectiveOwner.getEmail(), response)  ← Owner-Channel
webSocketHandler.broadcastActivityLog(effectiveOwner.getEmail(), logEntry)
```

**Kein DB-Schema-Change nötig**: `ActivityLog.actorName` ist bereits vorhanden.

---

## BR-07: Mitglied entfernen

**Regel**: Wenn ein Owner ein Mitglied entfernt:

1. Die Zeile in `home_members` wird gelöscht
2. Der Ex-Member ist wieder OWNER seines eigenen Homes (seine Räume/Geräte bleiben in der DB erhalten)
3. Aktive Requests des Ex-Members mit altem JWT liefern beim nächsten Request OWNER-Kontext (eigene Räume)

**Constraint**: Ein Owner kann nur seine eigenen Mitglieder entfernen (`home_members.owner_id = owner.id`).

---

## BR-08: Registrierung

**Regel**: Jeder neu registrierte User erhält automatisch die Rolle OWNER (es wird kein Eintrag in `home_members` angelegt). `AuthResponse` enthält `role = "OWNER"`.

---

## BR-09: Login

**Regel**: Bei jedem Login wird die aktuelle Rolle aus `home_members` gelesen und in `AuthResponse.role` zurückgegeben (`"OWNER"` oder `"MEMBER"`).
