-- =============================================
-- V5__seed_sample_users.sql
-- Sample admins, staff with store access & permissions
-- =============================================

-- =============================================
-- Add a second store (branch) under Default Organization
-- =============================================
INSERT INTO stores (id, org_id, name, address, city, state, pincode, phone) VALUES
    (2, 1, 'MG Road Branch', '45 MG Road, Lajpat Nagar', 'New Delhi', 'Delhi', '110024', '9111222333');

-- =============================================
-- Sample Users
-- =============================================

-- ADMIN for Default Store (password: branch123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change, active) VALUES
    (2, 1, 'Vikram Admin', '9876500001',
     '$2a$10$kF7kar3Mt5AL2hMUPCpzbOnCtr0WLlaY2G9HUZLLezvMYCcIr0JAu',
     'ADMIN', FALSE, TRUE);

-- ADMIN for MG Road Branch (password: branch123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change, active) VALUES
    (3, 1, 'Priya Branch Admin', '9876500002',
     '$2a$10$kF7kar3Mt5AL2hMUPCpzbOnCtr0WLlaY2G9HUZLLezvMYCcIr0JAu',
     'ADMIN', FALSE, TRUE);

-- STAFF with BILLING permissions only (password: staff123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change, active) VALUES
    (4, 1, 'Amit Billing Staff', '9876500003',
     '$2a$10$4Wxt3dJGoS.SCEsLUm1nheLS6EyfYO0DL6eRD8TNRTI/dnzzzyfkS',
     'STAFF', FALSE, TRUE);

-- STAFF with INVENTORY permissions only (password: staff123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change, active) VALUES
    (5, 1, 'Sunita Inventory Staff', '9876500004',
     '$2a$10$kCkwznf.VsJpL2yLJjVO5OcLA0QyEt8JXGKc6Vx8TVDS9rKILwkSC',
     'STAFF', FALSE, TRUE);

-- STAFF with VIEW-ONLY permissions (password: staff123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change, active) VALUES
    (6, 1, 'Rohan View-Only Staff', '9876500005',
     '$2a$10$4Wxt3dJGoS.SCEsLUm1nheLS6EyfYO0DL6eRD8TNRTI/dnzzzyfkS',
     'STAFF', FALSE, TRUE);

-- =============================================
-- Store Access
-- =============================================

-- Owner (user 1) already has access to store 1; give access to store 2
INSERT INTO user_store_access (user_id, store_id) VALUES (1, 2);

-- Vikram Admin → Default Store only
INSERT INTO user_store_access (user_id, store_id) VALUES (2, 1);

-- Priya Admin → MG Road Branch only
INSERT INTO user_store_access (user_id, store_id) VALUES (3, 2);

-- Amit Billing Staff → Default Store
INSERT INTO user_store_access (user_id, store_id) VALUES (4, 1);

-- Sunita Inventory Staff → Default Store
INSERT INTO user_store_access (user_id, store_id) VALUES (5, 1);

-- Rohan View-Only Staff → Both stores
INSERT INTO user_store_access (user_id, store_id) VALUES (6, 1);
INSERT INTO user_store_access (user_id, store_id) VALUES (6, 2);

-- =============================================
-- Staff Permissions (OWNER & ADMIN bypass these)
-- Permission IDs from V3 seed:
--   1=VIEW_INVENTORY, 2=MANAGE_INVENTORY,
--   3=VIEW_CUSTOMERS, 4=MANAGE_CUSTOMERS,
--   5=CREATE_INVOICE, 6=VIEW_INVOICES,
--   7=MANAGE_INVOICES, 8=VIEW_REPORTS,
--   9=MANAGE_STAFF
-- =============================================

-- Amit Billing Staff (store 1): Can view inventory, view/create/manage invoices, view customers
INSERT INTO user_permissions (user_id, store_id, permission_id) VALUES
    (4, 1, 1),  -- VIEW_INVENTORY
    (4, 1, 3),  -- VIEW_CUSTOMERS
    (4, 1, 5),  -- CREATE_INVOICE
    (4, 1, 6),  -- VIEW_INVOICES
    (4, 1, 7);  -- MANAGE_INVOICES

-- Sunita Inventory Staff (store 1): Can view/manage inventory, view customers
INSERT INTO user_permissions (user_id, store_id, permission_id) VALUES
    (5, 1, 1),  -- VIEW_INVENTORY
    (5, 1, 2),  -- MANAGE_INVENTORY
    (5, 1, 3),  -- VIEW_CUSTOMERS
    (5, 1, 4);  -- MANAGE_CUSTOMERS

-- Rohan View-Only Staff (store 1): Can only view everything
INSERT INTO user_permissions (user_id, store_id, permission_id) VALUES
    (6, 1, 1),  -- VIEW_INVENTORY
    (6, 1, 3),  -- VIEW_CUSTOMERS
    (6, 1, 6),  -- VIEW_INVOICES
    (6, 1, 8);  -- VIEW_REPORTS

-- Rohan View-Only Staff (store 2): Can only view inventory
INSERT INTO user_permissions (user_id, store_id, permission_id) VALUES
    (6, 2, 1);  -- VIEW_INVENTORY

-- =============================================
-- Seed some data for Store 2 (MG Road Branch)
-- =============================================
INSERT INTO categories (store_id, name, description) VALUES
    (2, 'Rings', 'All types of rings'),
    (2, 'Necklaces', 'Chains and necklaces'),
    (2, 'Earrings', 'Studs, drops, and hoops');

INSERT INTO metal_types (store_id, name, purity, current_rate, unit) VALUES
    (2, 'Gold', '22K', 7100.00, 'gram'),
    (2, 'Gold', '18K', 5800.00, 'gram'),
    (2, 'Silver', '925', 98.00, 'gram');
