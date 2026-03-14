-- =============================================
-- V3__multi_tenant_auth.sql
-- Multi-tenant architecture + Authentication
-- =============================================

-- =============================================
-- Organizations (top-level business entity)
-- =============================================
CREATE TABLE organizations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Stores (branches under an organization)
-- =============================================
CREATE TABLE stores (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id      BIGINT       NOT NULL,
    name        VARCHAR(200) NOT NULL,
    address     VARCHAR(500),
    city        VARCHAR(100),
    state       VARCHAR(100),
    pincode     VARCHAR(10),
    phone       VARCHAR(15),
    gstin       VARCHAR(20),
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_org (org_id),
    CONSTRAINT fk_store_org FOREIGN KEY (org_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Users (owners, admins, staff)
-- =============================================
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id          BIGINT       NOT NULL,
    name            VARCHAR(150) NOT NULL,
    mobile          VARCHAR(15)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('OWNER', 'ADMIN', 'STAFF') NOT NULL,
    force_password_change BOOLEAN DEFAULT TRUE,
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_mobile (mobile),
    KEY idx_user_org (org_id),
    CONSTRAINT fk_user_org FOREIGN KEY (org_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- User ↔ Store access (which stores can a user access)
-- =============================================
CREATE TABLE user_store_access (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    store_id    BIGINT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_store (user_id, store_id),
    CONSTRAINT fk_usa_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_usa_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Permissions (master list)
-- =============================================
CREATE TABLE permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    UNIQUE KEY uk_permission_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- User ↔ Permissions (per user, per store)
-- =============================================
CREATE TABLE user_permissions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    permission_id   BIGINT NOT NULL,
    UNIQUE KEY uk_user_store_perm (user_id, store_id, permission_id),
    CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_up_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_up_perm FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Seed Permissions
-- =============================================
INSERT INTO permissions (name, description) VALUES
    ('VIEW_INVENTORY',    'View jewelry items, categories, metals'),
    ('MANAGE_INVENTORY',  'Add/edit/delete inventory items'),
    ('VIEW_CUSTOMERS',    'View customer list'),
    ('MANAGE_CUSTOMERS',  'Add/edit customers'),
    ('CREATE_INVOICE',    'Create new invoices/bills'),
    ('VIEW_INVOICES',     'View existing invoices'),
    ('MANAGE_INVOICES',   'Edit/cancel invoices'),
    ('VIEW_REPORTS',      'View sales reports, stock reports'),
    ('MANAGE_STAFF',      'Add/remove staff members');

-- =============================================
-- Seed default Organization + Store for existing data
-- =============================================
INSERT INTO organizations (id, name) VALUES (1, 'Default Organization');
INSERT INTO stores (id, org_id, name) VALUES (1, 1, 'Default Store');

-- Seed default OWNER user (password: admin123 — BCrypt hash)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change)
VALUES (1, 1, 'Admin', '9999999999',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'OWNER', TRUE);

-- Give OWNER access to default store
INSERT INTO user_store_access (user_id, store_id) VALUES (1, 1);

-- =============================================
-- Add store_id to all existing tables
-- =============================================

-- Categories
ALTER TABLE categories ADD COLUMN store_id BIGINT NULL AFTER id;
UPDATE categories SET store_id = 1 WHERE store_id IS NULL;
ALTER TABLE categories MODIFY COLUMN store_id BIGINT NOT NULL;
ALTER TABLE categories ADD CONSTRAINT fk_category_store FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE categories DROP INDEX uk_category_name;
ALTER TABLE categories ADD UNIQUE KEY uk_category_name_store (name, store_id);
ALTER TABLE categories ADD KEY idx_category_store (store_id);

-- Metal Types
ALTER TABLE metal_types ADD COLUMN store_id BIGINT NULL AFTER id;
UPDATE metal_types SET store_id = 1 WHERE store_id IS NULL;
ALTER TABLE metal_types MODIFY COLUMN store_id BIGINT NOT NULL;
ALTER TABLE metal_types ADD CONSTRAINT fk_metal_type_store FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE metal_types DROP INDEX uk_metal_purity;
ALTER TABLE metal_types ADD UNIQUE KEY uk_metal_purity_store (name, purity, store_id);
ALTER TABLE metal_types ADD KEY idx_metal_type_store (store_id);

-- Jewelry Items
ALTER TABLE jewelry_items ADD COLUMN store_id BIGINT NULL AFTER id;
UPDATE jewelry_items SET store_id = 1 WHERE store_id IS NULL;
ALTER TABLE jewelry_items MODIFY COLUMN store_id BIGINT NOT NULL;
ALTER TABLE jewelry_items ADD CONSTRAINT fk_jewelry_item_store FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE jewelry_items DROP INDEX uk_sku;
ALTER TABLE jewelry_items ADD UNIQUE KEY uk_sku_store (sku, store_id);
ALTER TABLE jewelry_items ADD KEY idx_jewelry_item_store (store_id);

-- Customers
ALTER TABLE customers ADD COLUMN store_id BIGINT NULL AFTER id;
UPDATE customers SET store_id = 1 WHERE store_id IS NULL;
ALTER TABLE customers MODIFY COLUMN store_id BIGINT NOT NULL;
ALTER TABLE customers ADD CONSTRAINT fk_customer_store FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE customers DROP INDEX uk_phone;
ALTER TABLE customers ADD UNIQUE KEY uk_phone_store (phone, store_id);
ALTER TABLE customers ADD KEY idx_customer_store (store_id);

-- Invoices
ALTER TABLE invoices ADD COLUMN store_id BIGINT NULL AFTER id;
UPDATE invoices SET store_id = 1 WHERE store_id IS NULL;
ALTER TABLE invoices MODIFY COLUMN store_id BIGINT NOT NULL;
ALTER TABLE invoices ADD CONSTRAINT fk_invoice_store FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE invoices DROP INDEX uk_invoice_number;
ALTER TABLE invoices ADD UNIQUE KEY uk_invoice_number_store (invoice_number, store_id);
ALTER TABLE invoices ADD KEY idx_invoice_store (store_id);

-- Invoice Items
ALTER TABLE invoice_items ADD COLUMN store_id BIGINT NULL AFTER id;
UPDATE invoice_items SET store_id = 1 WHERE store_id IS NULL;
ALTER TABLE invoice_items MODIFY COLUMN store_id BIGINT NOT NULL;
ALTER TABLE invoice_items ADD CONSTRAINT fk_invoice_item_store FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE invoice_items ADD KEY idx_invoice_item_store (store_id);
