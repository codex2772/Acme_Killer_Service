-- ============================================================
-- V6__super_admin_feature_modules.sql
-- Super Admin platform, feature modules, subscriptions
-- ============================================================

-- Super admin users (AuraJewels team only, separate from regular users table)
CREATE TABLE platform_admins (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    phone         VARCHAR(15),
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('SUPER_ADMIN', 'SUPPORT') NOT NULL DEFAULT 'SUPER_ADMIN',
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Feature modules master list
CREATE TABLE feature_modules (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_core     BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order  INT NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Which modules are enabled for which store
CREATE TABLE store_feature_modules (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id    BIGINT NOT NULL,
    module_id   BIGINT NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    enabled_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enabled_by  BIGINT,
    UNIQUE KEY uk_store_module (store_id, module_id),
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (module_id) REFERENCES feature_modules(id)
);

-- Subscription / plan tracking per organization
CREATE TABLE org_subscriptions (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id               BIGINT NOT NULL,
    plan                 ENUM('STARTER', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM') NOT NULL DEFAULT 'STARTER',
    status               ENUM('ACTIVE', 'TRIAL', 'SUSPENDED', 'CANCELLED') NOT NULL DEFAULT 'TRIAL',
    trial_ends_at        TIMESTAMP NULL,
    current_period_start TIMESTAMP NULL,
    current_period_end   TIMESTAMP NULL,
    max_stores           INT NOT NULL DEFAULT 1,
    max_users            INT NOT NULL DEFAULT 2,
    notes                TEXT,
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (org_id) REFERENCES organizations(id)
);

-- Audit log for super admin actions
CREATE TABLE platform_audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id    BIGINT NOT NULL,
    admin_name  VARCHAR(100),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   BIGINT,
    detail      TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES platform_admins(id)
);

-- Add status/onboarding columns to organizations
ALTER TABLE organizations ADD COLUMN onboarded_by BIGINT NULL;
ALTER TABLE organizations ADD COLUMN onboarded_at TIMESTAMP NULL;
ALTER TABLE organizations ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Add status to stores
ALTER TABLE stores ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- Seed the feature modules
INSERT INTO feature_modules (code, name, description, is_core, sort_order) VALUES
('DASHBOARD',     'Dashboard',            'Sales summary and metrics',                   TRUE,  1),
('INVENTORY',     'Inventory Management', 'Items, categories, metal types',              FALSE, 2),
('BILLING',       'Billing & Invoicing',  'Invoices, estimates, credit notes',           FALSE, 3),
('CUSTOMERS',     'Customer Management',  'Customer database and enquiries',             FALSE, 4),
('ACCOUNTS',      'Accounts',             'Ledger, expenses, cash register, suppliers',  FALSE, 5),
('RATES',         'Daily Rates',          'Gold, silver, platinum rate management',      FALSE, 6),
('SCHEMES',       'Saving Schemes',       'Customer saving scheme management',           FALSE, 7),
('REPORTS',       'Reports & Analytics',  'Activity logs, advanced reports',             FALSE, 8),
('NOTIFICATIONS', 'Notifications',        'Bulk SMS, push notifications',                FALSE, 9),
('CUSTOMER_APP',  'Customer App',         'Customer mobile app catalog for this store',  FALSE, 10),
('SETTINGS',      'Settings',             'Store profile and configuration',             TRUE,  11);

-- Enable ALL modules for all existing stores (backward compatibility)
INSERT INTO store_feature_modules (store_id, module_id, enabled)
SELECT s.id, fm.id, TRUE
FROM stores s CROSS JOIN feature_modules fm;
