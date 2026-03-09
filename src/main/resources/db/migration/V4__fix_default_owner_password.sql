-- =============================================
-- V4__fix_default_owner_password.sql
-- Fix BCrypt hash for default OWNER user
-- Password: admin123
-- =============================================
UPDATE users
SET password_hash = '$2a$10$N5mlLobUQ6gi9j45vgfNx.zvVgLcG.qxCODaXiA1qi7H.vNmHZC7W'
WHERE mobile = '9999999999' AND role = 'OWNER';
