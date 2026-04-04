-- ═══════════════════════════════════════════
-- V9: Add Enquiries, Old Gold, Staff feature modules
-- ═══════════════════════════════════════════

-- 1. Insert new feature modules
INSERT IGNORE INTO feature_modules (code, name, description, is_core, sort_order) VALUES
('ENQUIRIES', 'Enquiries',        'Customer enquiry management and responses',      FALSE, 13),
('OLD_GOLD',  'Old Gold',         'Old gold purchase, purity test, and melting',     FALSE, 14),
('STAFF',     'Staff Management', 'Staff CRUD, role assignment, and permissions',   FALSE, 15);

-- 2. Enable new modules for all existing stores (backward compatibility)
INSERT INTO store_feature_modules (store_id, module_id, enabled)
SELECT s.id, fm.id, TRUE
FROM stores s CROSS JOIN feature_modules fm
WHERE fm.code IN ('ENQUIRIES', 'OLD_GOLD', 'STAFF')
ON DUPLICATE KEY UPDATE enabled = TRUE;
