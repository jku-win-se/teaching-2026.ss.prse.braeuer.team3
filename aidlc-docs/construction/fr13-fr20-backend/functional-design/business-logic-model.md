# Business Logic Model — FR-13/FR-20 Backend

## Übersicht: Neue und geänderte Komponenten

```
MemberController  ──→  MemberService  ──→  HomeMemberRepository
                              │
                              └──→  UserRepository

DeviceService (mod.) ──→  MemberService.resolveEffectiveOwner()
RoomService   (mod.) ──→  MemberService.requireOwnerRole()
RuleService   (mod.) ──→  MemberService.requireOwnerRole()
ScheduleService(mod.)──→  MemberService.requireOwnerRole()

AuthService   (mod.) ──→  HomeMemberRepository (für role in AuthResponse)
```

---

## MemberService — Kern-Algorithmen

### `inviteMember(ownerEmail, inviteEmail)`
```
1. owner = userRepo.findByEmail(ownerEmail) → 401 if not found
2. requireOwnerRole(ownerEmail)             → 403 if caller is Member
3. invitee = userRepo.findByEmail(inviteEmail) → 404 "User not found."
4. if invitee.id == owner.id               → 400 "Cannot invite yourself."
5. if homeMemberRepo.findByMember(invitee).isPresent() → 409 "User is already a member of a home."
6. homeMember = new HomeMember(owner, invitee)
7. save(homeMember) → return MemberResponse
```

### `getMembers(ownerEmail)`
```
1. owner = userRepo.findByEmail(ownerEmail) → 401 if not found
2. requireOwnerRole(ownerEmail)             → 403 if caller is Member
3. return homeMemberRepo.findByOwner(owner).map(toResponse)
```

### `removeMember(ownerEmail, memberId)`
```
1. owner = userRepo.findByEmail(ownerEmail) → 401 if not found
2. requireOwnerRole(ownerEmail)             → 403 if caller is Member
3. membership = homeMemberRepo.findByOwnerAndMemberId(owner, memberId)
               → 404 "Member not found." if absent
4. delete(membership)
```

### `resolveEffectiveOwner(callerEmail)` — shared helper
```
1. caller = userRepo.findByEmail(callerEmail) → 401 if not found
2. membership = homeMemberRepo.findByMember(caller)
3. return membership.map(HomeMember::getOwner).orElse(caller)
```

### `requireOwnerRole(callerEmail)` — guard helper
```
1. caller = userRepo.findByEmail(callerEmail) → 401 if not found
2. if homeMemberRepo.findByMember(caller).isPresent()
   → throw 403 "Access denied: Owner role required."
```

---

## DeviceService — Änderungen

### `getDevices(email, roomId)` — Lesen (Member erlaubt)
```diff
- Room room = getOwnedRoom(email, roomId);
+ Room room = getEffectiveRoom(email, roomId);   // Owner-Kontext für Member
```

### `addDevice(email, roomId, request)` — OWNER only
```diff
+ memberService.requireOwnerRole(email);   // 403 für Member
  Room room = getOwnedRoom(email, roomId);
```

### `renameDevice(email, roomId, deviceId, request)` — OWNER only
```diff
+ memberService.requireOwnerRole(email);
  Room room = getOwnedRoom(email, roomId);
```

### `deleteDevice(email, roomId, deviceId)` — OWNER only
```diff
+ memberService.requireOwnerRole(email);
  Room room = getOwnedRoom(email, roomId);
```

### `updateState(email, roomId, deviceId, request)` — Member erlaubt
```diff
  Room room = getEffectiveRoom(email, roomId);
  User caller = userRepo.findByEmail(email) → 401
+ User effectiveOwner = memberService.resolveEffectiveOwner(email);
  Device device = deviceRepo.findByIdAndRoomId(deviceId, room.getId()) → 404
  applyStateFields(device, request);
  DeviceResponse response = toResponse(deviceRepo.save(device));
- webSocketHandler.broadcast(email, response);
+ webSocketHandler.broadcast(effectiveOwner.getEmail(), response);   // Owner-Channel
  String action = activityLogService.buildActionDescription(device, request);
- ActivityLogResponse logEntry = activityLogService.log(device, resolvedUser, resolvedUser.getName(), action);
+ ActivityLogResponse logEntry = activityLogService.log(device, effectiveOwner, caller.getName(), action);
- webSocketHandler.broadcastActivityLog(email, logEntry);
+ webSocketHandler.broadcastActivityLog(effectiveOwner.getEmail(), logEntry);
  ruleService.evaluateRulesForDevice(device, request, stateOnChanged);
```

### Neue private Methode `getEffectiveRoom(email, roomId)`
```java
private Room getEffectiveRoom(String email, Long roomId) {
    User effectiveOwner = memberService.resolveEffectiveOwner(email);
    return roomRepository.findByIdAndUserId(roomId, effectiveOwner.getId())
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Room not found."));
}
```

---

## RoomService — Änderungen

| Methode | Änderung |
|---------|---------|
| `getRooms(email)` | `user` → `resolveEffectiveOwner(email)` |
| `getRoom(email, id)` | `user` → `resolveEffectiveOwner(email)` |
| `createRoom(email, req)` | `requireOwnerRole(email)` vor allem anderen |
| `renameRoom(email, id, req)` | `requireOwnerRole(email)` |
| `deleteRoom(email, id)` | `requireOwnerRole(email)` |

---

## RuleService — Änderungen

Alle Methoden (getRules, createRule, updateRule, deleteRule, evaluateRulesForDevice):
- Read-Operationen: `requireOwnerRole(email)` (Member sieht keine Regeln)
- Write-Operationen: `requireOwnerRole(email)`

**Ausnahme**: `evaluateRulesForDevice(device, request, stateOnChanged)` — wird intern ohne User-Email aufgerufen, bleibt unverändert.

---

## ScheduleService — Änderungen

Alle Methoden (getSchedules, createSchedule, updateSchedule, deleteSchedule):
- `requireOwnerRole(email)` am Anfang jeder Methode

---

## AuthService — Änderungen

### `register(request)` — Rolle immer OWNER
```diff
  return new AuthResponse(token, user.getName(), user.getEmail());
+ // erweitert: role = "OWNER" (kein home_members Eintrag)
```

### `login(request)` — Rolle aus DB lesen
```diff
  String token = jwtUtil.generateToken(user.getEmail());
+ String role = homeMemberRepo.findByMember(user).isPresent() ? "MEMBER" : "OWNER";
- return new AuthResponse(token, user.getName(), user.getEmail());
+ return new AuthResponse(token, user.getName(), user.getEmail(), role);
```

---

## Sequenzdiagramm: Member ändert Gerätezustand

```
Member (JWT)    DeviceController    DeviceService    MemberService    DB
    │                │                   │                │            │
    │ PATCH /rooms/{r}/devices/{d}/state │                │            │
    │─────────────────────────────────>  │                │            │
    │                │ updateState(member@,r,d,req)       │            │
    │                │──────────────────>│                │            │
    │                │                   │ resolveEffectiveOwner(member@)
    │                │                   │───────────────>│            │
    │                │                   │                │ findByMember(member)
    │                │                   │                │──────────>│
    │                │                   │                │<── HomeMember(owner) ─
    │                │                   │<── owner User ─│            │
    │                │                   │ findByIdAndUserId(r, owner.id)
    │                │                   │─────────────────────────────>│
    │                │                   │<── Room (owner's room) ──────│
    │                │                   │ save(device), broadcast(owner@)
    │                │                   │ log(device, owner, member.name, action)
    │                │                   │ broadcastActivityLog(owner@)
    │                │<── DeviceResponse ─│
    │<── 200 OK ──────│
```

---

## Sequenzdiagramm: Owner lädt Member ein

```
Owner (JWT)    MemberController    MemberService    UserRepo    HomeMemberRepo
    │                │                  │               │              │
    │ POST /api/members/invite {email}  │               │              │
    │───────────────>│                  │               │              │
    │                │ inviteMember(owner@, invite@)    │              │
    │                │─────────────────>│               │              │
    │                │                  │ requireOwnerRole(owner@)     │
    │                │                  │ findByMember(owner) → empty → OK
    │                │                  │ findByEmail(invite@)         │
    │                │                  │──────────────>│              │
    │                │                  │<── invitee ───│              │
    │                │                  │ findByMember(invitee) → empty → OK
    │                │                  │──────────────────────────>   │
    │                │                  │<── empty ─────────────────── │
    │                │                  │ save(HomeMember(owner,invitee))
    │                │                  │──────────────────────────>   │
    │                │<── MemberResponse│
    │<── 201 Created ─│
```

---

## Flyway Migration V9

```sql
CREATE TABLE home_members
(
    id         BIGSERIAL PRIMARY KEY,
    owner_id   BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    member_id  BIGINT    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    joined_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_home_members_member  UNIQUE (member_id),
    CONSTRAINT chk_home_members_no_self CHECK (owner_id <> member_id)
);

CREATE INDEX idx_home_members_owner_id ON home_members(owner_id);
```

---

## Neue REST-Endpunkte

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| `POST` | `/api/members/invite` | `MemberInviteRequest` | `MemberResponse` | 201 |
| `GET` | `/api/members` | — | `List<MemberResponse>` | 200 |
| `DELETE` | `/api/members/{memberId}` | — | — | 204 |

Alle Endpunkte: OWNER only (403 für Member).
