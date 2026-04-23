# Code Generation Plan — Bugfix #63: Email Case-Insensitivity

## Unit Context

- **Unit Name**: bugfix-email-validation
- **Branch**: `63-bugfix-email-validation`
- **Bug Reference**: Issue #63
- **Source Plan**: `localDocs/63-bugfix-email-validation-plan.md`
- **Scope**: Backend only — `AuthService.java`
- **Type**: Brownfield modification (modify existing files, no new files created)

## Stories / Acceptance Criteria

- [ ] **AC-1**: Registering with `User@Example.com` and then registering again with `user@example.com` is rejected as a duplicate
- [ ] **AC-2**: Registering with `User@Example.com` and logging in with `user@example.com` succeeds
- [ ] **AC-3**: All-lowercase email registration and login continue to work (regression-safe)

## Dependencies

- No dependency on other units
- No schema changes required (existing UNIQUE constraint is sufficient)
- No frontend changes required (backend is the source of truth)

## Affected Files

| File | Action |
|------|--------|
| `backend/src/main/java/at/jku/se/smarthome/service/AuthService.java` | **Modify** — normalize email in `register()` and `login()` |
| `backend/src/test/java/at/jku/se/smarthome/service/AuthServiceTest.java` | **Modify** — add test cases for case-insensitive registration and login |

---

## Step-by-Step Plan

### Step 1 — Modify `AuthService.register()` [x]
In `AuthService.java`, locate the `register()` method.
- Before the `existsByEmail()` call, normalize the email:
  ```java
  String email = request.getEmail().toLowerCase(Locale.ROOT).strip();
  ```
- Use this normalized `email` variable for all subsequent operations (duplicate check + saving to entity).
- Add `import java.util.Locale;` if not already present.
- Ensure no PMD violations (no unused variables, no `System.out.println`).
- Ensure existing Javadoc on `register()` is updated if the behaviour description changes.

### Step 2 — Modify `AuthService.login()` [x]
In `AuthService.java`, locate the `login()` method.
- Normalize the email before the `findByEmail()` call:
  ```java
  String email = request.getEmail().toLowerCase(Locale.ROOT).strip();
  userRepository.findByEmail(email)...
  ```
- Ensure existing Javadoc on `login()` is updated if needed.

### Step 3 — Add / Update Unit Tests in `AuthServiceTest.java` [x]
Add the following test cases:
1. `registerWithMixedCaseEmail_shouldNormalizeAndStore` — registers with `User@Example.com`, verifies stored email is `user@example.com`
2. `registerDuplicateEmailDifferentCase_shouldRejectAsDuplicate` — attempts to register `user@example.com` after `User@Example.com` exists, expects duplicate rejection
3. `loginWithDifferentCaseThanRegistered_shouldSucceed` — registers `User@Example.com`, logs in with `user@example.com`, expects success

### Step 4 — PMD & Javadoc Review [x]
- Mentally review all modified methods for PMD compliance (no empty catch, no unused imports/variables, no `System.out.println`)
- Verify Javadoc is present and accurate on `register()` and `login()` in `AuthService`

---

## Completion Criteria

- [ ] Step 1 complete — `register()` normalizes email
- [ ] Step 2 complete — `login()` normalizes email
- [ ] Step 3 complete — unit tests added/updated
- [ ] Step 4 complete — PMD + Javadoc verified
- [ ] AC-1, AC-2, AC-3 satisfied
