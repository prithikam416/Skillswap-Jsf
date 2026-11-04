-- ============================================================
-- Person 3 upgrade script (OPTIONAL)
-- The application runs this upgrade by itself the first time it connects
-- (see DBUtil.ensureSchema), so you normally do NOT need to run this file.
-- Use it only if you want to upgrade an existing database by hand.
-- Run once; if MySQL says "Duplicate column name", that column already exists - ignore it.
-- ============================================================
USE skillswap_db;

ALTER TABLE exchange_requests ADD COLUMN session_date DATETIME NULL;
ALTER TABLE exchange_requests ADD COLUMN session_mode VARCHAR(20) NULL;
ALTER TABLE exchange_requests ADD COLUMN session_location VARCHAR(255) NULL;
ALTER TABLE exchange_requests ADD COLUMN response_note TEXT NULL;
