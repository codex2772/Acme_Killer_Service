-- =============================================
-- V3__fix_scheme_tables.sql
-- Add missing columns to scheme_members and scheme_payments
-- BaseEntity requires: id, active, created_at, updated_at
-- =============================================

-- scheme_members: add missing 'active' column
ALTER TABLE scheme_members ADD COLUMN active BOOLEAN DEFAULT TRUE;

-- scheme_payments: add missing 'active' and 'updated_at' columns
ALTER TABLE scheme_payments ADD COLUMN active BOOLEAN DEFAULT TRUE;
ALTER TABLE scheme_payments ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
