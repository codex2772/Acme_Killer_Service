-- =============================================
-- V3__fix_scheme_tables.sql
-- Add missing columns to scheme_members and scheme_payments
-- BaseEntity requires: id, active, created_at, updated_at
--
-- V1 was later amended to create these columns directly, so on a database
-- built from scratch they already exist by the time this runs. The guards
-- below keep this migration a no-op in that case while still patching any
-- database created from the original V1.
-- =============================================

-- scheme_members: add missing 'active' column
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scheme_members' AND COLUMN_NAME = 'active');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE scheme_members ADD COLUMN active BOOLEAN DEFAULT TRUE',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- scheme_payments: add missing 'active' column
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scheme_payments' AND COLUMN_NAME = 'active');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE scheme_payments ADD COLUMN active BOOLEAN DEFAULT TRUE',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- scheme_payments: add missing 'updated_at' column
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'scheme_payments' AND COLUMN_NAME = 'updated_at');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE scheme_payments ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
