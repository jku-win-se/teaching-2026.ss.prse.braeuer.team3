-- V10: Migrate existing rooms and rules from user-owned to household-owned model (US-015)
-- For CI environments with no existing data this script is a no-op.

-- Step 1: Add nullable household_id columns to rooms and rules
ALTER TABLE rooms ADD COLUMN household_id BIGINT REFERENCES household(id) ON DELETE CASCADE;
ALTER TABLE rules ADD COLUMN household_id BIGINT REFERENCES household(id) ON DELETE CASCADE;

-- Step 2: Create a household for every user who owns at least one room,
--         then wire up rooms, rules, and membership in a single PL/pgSQL block.
DO $$
DECLARE
    r_user         RECORD;
    v_household_id BIGINT;
BEGIN
    FOR r_user IN
        SELECT DISTINCT u.id, u.name
        FROM   users u
        WHERE  EXISTS (SELECT 1 FROM rooms r WHERE r.user_id = u.id)
    LOOP
        INSERT INTO household (name, created_at)
        VALUES (r_user.name || '''s Haushalt', NOW())
        RETURNING id INTO v_household_id;

        INSERT INTO household_member (household_id, user_id, role, joined_at)
        VALUES (v_household_id, r_user.id, 'OWNER', NOW());

        UPDATE rooms SET household_id = v_household_id WHERE user_id = r_user.id;
        UPDATE rules SET household_id = v_household_id WHERE user_id = r_user.id;
    END LOOP;
END $$;

-- Step 3: Remove obsolete user_id columns
ALTER TABLE rooms DROP COLUMN user_id;
ALTER TABLE rules DROP COLUMN user_id;
