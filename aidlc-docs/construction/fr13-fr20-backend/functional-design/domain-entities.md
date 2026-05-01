# Domain Entities — FR-13/FR-20 Backend

## Bestehende Entities (unverändert)

- `User` — keine Änderung am Schema (Rolle wird aus `home_members` abgeleitet, kein `role`-Feld nötig)
- `ActivityLog` — bereits `actorName`-Feld vorhanden, keine DB-Änderung nötig

---

## Neue Entity: `HomeMember`

**Tabelle**: `home_members`

| Feld | Typ | Constraint | Beschreibung |
|------|-----|-----------|--------------|
| `id` | `BIGSERIAL` | PK | Primärschlüssel |
| `owner` | `User` (FK → users.id) | NOT NULL, ON DELETE CASCADE | Der einladende Owner |
| `member` | `User` (FK → users.id) | NOT NULL, UNIQUE, ON DELETE CASCADE | Das eingeladene Mitglied |
| `joinedAt` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | Zeitpunkt der Einladung |

**Constraints:**
- `UNIQUE(member_id)` — exklusives Rollenmodell: jeder User kann nur in einem Home Member sein
- `CHECK(owner_id != member_id)` — kein Selbst-Einladen
- Index auf `owner_id` für performante Mitgliederlisten-Abfragen

**Beziehungen:**
```
User (owner) ──< HomeMember >── User (member)
                   [1 owner : 0..* members]
                   [1 member : 0..1 membership]   ← UNIQUE Constraint
```

---

## DTO-Änderungen

### `AuthResponse` (bestehend, erweitert)
Neues Feld: `role` (`String`: `"OWNER"` oder `"MEMBER"`)

Der Login/Register-Endpunkt gibt die Rolle zurück, damit das Frontend sofort Verwaltungs-Buttons ein-/ausblenden kann.

### `MemberInviteRequest` (neu)
```
email: String  (E-Mail-Adresse des einzuladenden Users, @NotBlank, @Email)
```

### `MemberResponse` (neu)
```
id:        Long    (ID des Member-Users)
name:      String  (Anzeigename)
email:     String  (E-Mail-Adresse)
joinedAt:  Instant (Zeitpunkt der Einladung)
```

---

## Rollenauflösung (kein `role`-Feld in `users`)

Die Rolle eines Users wird **on-demand** aus `home_members` abgeleitet:

```
OWNER: home_members enthält KEINE Zeile mit member_id = user.id
MEMBER: home_members enthält eine Zeile mit member_id = user.id
```

→ Vorteil: Widerruf wirkt sofort, kein invalidierter JWT nötig.
→ `HomeMemberRepository.findByMember(User member)` → `Optional<HomeMember>`
