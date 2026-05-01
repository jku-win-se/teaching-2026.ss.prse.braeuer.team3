-- V11: Restore the user-owned rooms/rules schema and create home_members.
-- V9/V10 are preserved for Flyway history compatibility with earlier local runs.

ALTER TABLE rooms ADD COLUMN user_id BIGINT REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE rules ADD COLUMN user_id BIGINT REFERENCES users(id) ON DELETE CASCADE;

UPDATE rooms r
SET user_id = hm.user_id
FROM household_member hm
WHERE r.household_id = hm.household_id
  AND hm.role = 'OWNER';

UPDATE rules ru
SET user_id = hm.user_id
FROM household_member hm
WHERE ru.household_id = hm.household_id
  AND hm.role = 'OWNER';

DELETE FROM rules WHERE user_id IS NULL;
DELETE FROM rooms WHERE user_id IS NULL;

ALTER TABLE rooms ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE rules ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE rooms ADD CONSTRAINT uq_rooms_user_name UNIQUE (user_id, name);
CREATE INDEX idx_rooms_user_id ON rooms (user_id);

ALTER TABLE rooms DROP COLUMN household_id;
ALTER TABLE rules DROP COLUMN household_id;

CREATE TABLE home_members
(
    id        BIGSERIAL PRIMARY KEY,
    owner_id  BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    member_id BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_home_members_member  UNIQUE (member_id),
    CONSTRAINT chk_home_members_no_self CHECK (owner_id <> member_id)
);

INSERT INTO home_members (owner_id, member_id, joined_at)
SELECT owner_member.user_id, invited_member.user_id, invited_member.joined_at
FROM household_member invited_member
JOIN household_member owner_member
  ON owner_member.household_id = invited_member.household_id
 AND owner_member.role = 'OWNER'
WHERE invited_member.role = 'MEMBER'
  AND invited_member.user_id <> owner_member.user_id
ON CONFLICT DO NOTHING;

CREATE INDEX idx_home_members_owner_id ON home_members (owner_id);
