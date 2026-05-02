-- V12: Allow invited users to be either OWNER or MEMBER inside a shared home.

ALTER TABLE home_members
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';

ALTER TABLE home_members
    ADD CONSTRAINT chk_home_members_role CHECK (role IN ('OWNER', 'MEMBER'));
