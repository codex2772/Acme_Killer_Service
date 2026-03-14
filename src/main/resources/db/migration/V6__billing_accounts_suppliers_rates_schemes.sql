-- =============================================
-- V6__billing_accounts_suppliers_rates_schemes.sql
-- All new modules: Billing, Accounts, Suppliers,
-- Rates, Old Gold, Schemes, Settings, Activity Logs
-- =============================================

-- =============================================
-- New Permissions
-- =============================================
INSERT INTO permissions (name, description) VALUES
    ('VIEW_BILLING',      'View invoices, estimates, credit notes'),
    ('MANAGE_BILLING',    'Create/update invoices, estimates, credit notes'),
    ('VIEW_ACCOUNTS',     'View ledger, expenses, suppliers, cash register'),
    ('MANAGE_ACCOUNTS',   'Create/update ledger entries, expenses, suppliers'),
    ('MANAGE_RATES',      'Update daily metal rates'),
    ('MANAGE_OLD_GOLD',   'Record old gold purchases/exchanges'),
    ('MANAGE_SCHEMES',    'Create/manage saving schemes and members');

-- =============================================
-- Alter existing invoices table to add new columns
-- =============================================
ALTER TABLE invoices ADD COLUMN store_id BIGINT AFTER id;
ALTER TABLE invoices ADD COLUMN active BOOLEAN DEFAULT TRUE AFTER status;
ALTER TABLE invoices ADD COLUMN gst_rate DECIMAL(5,2) NOT NULL DEFAULT 3.00 AFTER discount;
ALTER TABLE invoices ADD COLUMN gst_amount DECIMAL(14,2) NOT NULL DEFAULT 0 AFTER gst_rate;
ALTER TABLE invoices ADD COLUMN round_off DECIMAL(10,2) NOT NULL DEFAULT 0 AFTER gst_amount;
ALTER TABLE invoices ADD COLUMN old_gold_adjustment DECIMAL(14,2) NOT NULL DEFAULT 0 AFTER round_off;
ALTER TABLE invoices ADD COLUMN digital_signature VARCHAR(200) AFTER notes;
ALTER TABLE invoices ADD COLUMN due_date DATE AFTER payment_mode;
ALTER TABLE invoices ADD COLUMN created_by BIGINT AFTER active;
ALTER TABLE invoices ADD COLUMN invoice_type ENUM('INVOICE','ESTIMATE','CREDIT_NOTE') DEFAULT 'INVOICE' AFTER store_id;

-- Set store_id to 1 for existing invoices
UPDATE invoices SET store_id = 1 WHERE store_id IS NULL;
ALTER TABLE invoices MODIFY store_id BIGINT NOT NULL;
ALTER TABLE invoices ADD CONSTRAINT fk_inv_store FOREIGN KEY (store_id) REFERENCES stores(id);
ALTER TABLE invoices ADD KEY idx_inv_store (store_id);

-- Add store_id to existing invoice_items table
ALTER TABLE invoice_items ADD COLUMN store_id BIGINT AFTER invoice_id;
UPDATE invoice_items SET store_id = 1 WHERE store_id IS NULL;
ALTER TABLE invoice_items MODIFY store_id BIGINT NOT NULL;
ALTER TABLE invoice_items ADD CONSTRAINT fk_ii_store FOREIGN KEY (store_id) REFERENCES stores(id);

-- =============================================
-- Invoice Payments (split payments)
-- =============================================
CREATE TABLE invoice_payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id      BIGINT         NOT NULL,
    store_id        BIGINT         NOT NULL,
    mode            VARCHAR(30)    NOT NULL COMMENT 'CASH, UPI, CARD, BANK_TRANSFER',
    amount          DECIMAL(14,2)  NOT NULL,
    reference       VARCHAR(100)   COMMENT 'Transaction reference',
    payment_date    DATE           NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ip_invoice (invoice_id),
    KEY idx_ip_store (store_id),
    CONSTRAINT fk_ip_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_ip_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Estimates
-- =============================================
CREATE TABLE estimates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    estimate_number VARCHAR(30)    NOT NULL,
    customer_id     BIGINT         NOT NULL,
    estimate_date   DATE           NOT NULL,
    subtotal        DECIMAL(14,2)  NOT NULL DEFAULT 0,
    discount        DECIMAL(14,2)  NOT NULL DEFAULT 0,
    gst_rate        DECIMAL(5,2)   NOT NULL DEFAULT 3.00,
    gst_amount      DECIMAL(14,2)  NOT NULL DEFAULT 0,
    round_off       DECIMAL(10,2)  NOT NULL DEFAULT 0,
    total_amount    DECIMAL(14,2)  NOT NULL DEFAULT 0,
    notes           TEXT,
    digital_signature VARCHAR(200),
    status          ENUM('DRAFT','SENT','ACCEPTED','EXPIRED','CONVERTED') DEFAULT 'DRAFT',
    converted_invoice_id BIGINT   NULL,
    valid_until     DATE,
    active          BOOLEAN DEFAULT TRUE,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_estimate_number_store (estimate_number, store_id),
    KEY idx_estimate_store (store_id),
    KEY idx_estimate_customer (customer_id),
    KEY idx_estimate_date (estimate_date),
    CONSTRAINT fk_estimate_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_estimate_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Estimate Line Items
-- =============================================
CREATE TABLE estimate_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    estimate_id     BIGINT         NOT NULL,
    store_id        BIGINT         NOT NULL,
    jewelry_item_id BIGINT,
    name            VARCHAR(200)   NOT NULL,
    weight          DECIMAL(10,3),
    purity          VARCHAR(20),
    rate            DECIMAL(12,2)  NOT NULL DEFAULT 0,
    making_charge   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    making_charge_type ENUM('PERCENTAGE','FLAT') DEFAULT 'PERCENTAGE',
    wastage         DECIMAL(5,2)   NOT NULL DEFAULT 0,
    amount          DECIMAL(14,2)  NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ei_estimate (estimate_id),
    CONSTRAINT fk_ei_estimate FOREIGN KEY (estimate_id) REFERENCES estimates(id),
    CONSTRAINT fk_ei_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Credit Notes
-- =============================================
CREATE TABLE credit_notes (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id            BIGINT         NOT NULL,
    credit_note_number  VARCHAR(30)    NOT NULL,
    customer_id         BIGINT         NOT NULL,
    original_invoice_id BIGINT,
    credit_note_date    DATE           NOT NULL,
    subtotal            DECIMAL(14,2)  NOT NULL DEFAULT 0,
    discount            DECIMAL(14,2)  NOT NULL DEFAULT 0,
    gst_rate            DECIMAL(5,2)   NOT NULL DEFAULT 3.00,
    gst_amount          DECIMAL(14,2)  NOT NULL DEFAULT 0,
    round_off           DECIMAL(10,2)  NOT NULL DEFAULT 0,
    total_amount        DECIMAL(14,2)  NOT NULL DEFAULT 0,
    return_reason       TEXT,
    refund_mode         ENUM('SAME_MODE','STORE_CREDIT','CASH_REFUND','BANK_TRANSFER') DEFAULT 'SAME_MODE',
    notes               TEXT,
    status              ENUM('DRAFT','ISSUED','APPLIED') DEFAULT 'DRAFT',
    active              BOOLEAN DEFAULT TRUE,
    created_by          BIGINT,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cn_number_store (credit_note_number, store_id),
    KEY idx_cn_store (store_id),
    KEY idx_cn_customer (customer_id),
    CONSTRAINT fk_cn_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_cn_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Credit Note Line Items
-- =============================================
CREATE TABLE credit_note_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    credit_note_id      BIGINT         NOT NULL,
    store_id            BIGINT         NOT NULL,
    jewelry_item_id     BIGINT,
    name                VARCHAR(200)   NOT NULL,
    weight              DECIMAL(10,3),
    purity              VARCHAR(20),
    rate                DECIMAL(12,2)  NOT NULL DEFAULT 0,
    making_charge       DECIMAL(12,2)  NOT NULL DEFAULT 0,
    making_charge_type  ENUM('PERCENTAGE','FLAT') DEFAULT 'PERCENTAGE',
    wastage             DECIMAL(5,2)   NOT NULL DEFAULT 0,
    amount              DECIMAL(14,2)  NOT NULL DEFAULT 0,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_cni_cn (credit_note_id),
    CONSTRAINT fk_cni_cn FOREIGN KEY (credit_note_id) REFERENCES credit_notes(id),
    CONSTRAINT fk_cni_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Ledger Entries
-- =============================================
CREATE TABLE ledger_entries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    entry_date      DATE           NOT NULL,
    party           VARCHAR(200)   NOT NULL,
    type            ENUM('CR','DR') NOT NULL,
    amount          DECIMAL(14,2)  NOT NULL,
    mode            VARCHAR(30)    NOT NULL,
    note            TEXT,
    category        VARCHAR(100),
    reference_id    VARCHAR(50),
    reference_type  VARCHAR(30) COMMENT 'INVOICE, CREDIT_NOTE, EXPENSE, OLD_GOLD',
    created_by      BIGINT,
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_le_store (store_id),
    KEY idx_le_date (entry_date),
    KEY idx_le_type (type),
    CONSTRAINT fk_le_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Expenses
-- =============================================
CREATE TABLE expenses (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    expense_date    DATE           NOT NULL,
    category        VARCHAR(100)   NOT NULL,
    description     TEXT,
    amount          DECIMAL(14,2)  NOT NULL,
    mode            VARCHAR(30)    NOT NULL,
    created_by      BIGINT,
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_exp_store (store_id),
    KEY idx_exp_date (expense_date),
    KEY idx_exp_category (category),
    CONSTRAINT fk_exp_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Cash Registers
-- =============================================
CREATE TABLE cash_registers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    register_date   DATE           NOT NULL,
    opening_balance DECIMAL(14,2)  NOT NULL DEFAULT 0,
    cash_in         DECIMAL(14,2)  NOT NULL DEFAULT 0,
    cash_out        DECIMAL(14,2)  NOT NULL DEFAULT 0,
    closing_balance DECIMAL(14,2),
    opened_by       BIGINT,
    closed_by       BIGINT,
    status          ENUM('OPEN','CLOSED') DEFAULT 'OPEN',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cr_store_date (store_id, register_date),
    KEY idx_cr_store (store_id),
    CONSTRAINT fk_cr_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Suppliers
-- =============================================
CREATE TABLE suppliers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    name            VARCHAR(200)   NOT NULL,
    phone           VARCHAR(15),
    email           VARCHAR(150),
    city            VARCHAR(100),
    address         VARCHAR(500),
    gst             VARCHAR(20),
    status          ENUM('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sup_store (store_id),
    CONSTRAINT fk_sup_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Supplier Metals (many-to-many simple)
-- =============================================
CREATE TABLE supplier_metals (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id     BIGINT         NOT NULL,
    metal           VARCHAR(50)    NOT NULL,
    KEY idx_sm_supplier (supplier_id),
    CONSTRAINT fk_sm_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Daily Rates
-- =============================================
CREATE TABLE daily_rates (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    rate_date       DATE           NOT NULL,
    gold_24k        DECIMAL(12,2)  NOT NULL DEFAULT 0,
    gold_22k        DECIMAL(12,2)  NOT NULL DEFAULT 0,
    gold_18k        DECIMAL(12,2)  NOT NULL DEFAULT 0,
    gold_14k        DECIMAL(12,2)  NOT NULL DEFAULT 0,
    silver          DECIMAL(12,2)  NOT NULL DEFAULT 0,
    platinum        DECIMAL(12,2)  NOT NULL DEFAULT 0,
    rhodium         DECIMAL(12,2)  NOT NULL DEFAULT 0,
    rose_gold_18k   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    white_gold_18k  DECIMAL(12,2)  NOT NULL DEFAULT 0,
    updated_by      VARCHAR(150),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dr_store_date (store_id, rate_date),
    KEY idx_dr_store (store_id),
    CONSTRAINT fk_dr_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Rate Alerts
-- =============================================
CREATE TABLE rate_alerts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    metal           VARCHAR(50)    NOT NULL,
    condition_type  ENUM('ABOVE','BELOW') NOT NULL,
    threshold       DECIMAL(12,2)  NOT NULL,
    customer_id     BIGINT,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ra_store (store_id),
    CONSTRAINT fk_ra_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_ra_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Old Gold Purchases
-- =============================================
CREATE TABLE old_gold_purchases (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    customer_id     BIGINT         NOT NULL,
    purchase_date   DATE           NOT NULL,
    weight          DECIMAL(10,3)  NOT NULL,
    purity          VARCHAR(20)    NOT NULL,
    rate            DECIMAL(12,2)  NOT NULL,
    total           DECIMAL(14,2)  NOT NULL,
    type            ENUM('PURCHASE','EXCHANGE') DEFAULT 'PURCHASE',
    kyc_done        BOOLEAN DEFAULT FALSE,
    notes           TEXT,
    active          BOOLEAN DEFAULT TRUE,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ogp_store (store_id),
    KEY idx_ogp_customer (customer_id),
    CONSTRAINT fk_ogp_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_ogp_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Old Gold Purity Tests
-- =============================================
CREATE TABLE old_gold_purity_tests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    old_gold_id     BIGINT         NOT NULL,
    method          VARCHAR(50)    NOT NULL COMMENT 'XRF, Acid Test, Fire Assay',
    actual_purity   VARCHAR(20),
    purity_percent  DECIMAL(5,2),
    tested_by       VARCHAR(150),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ogpt_og (old_gold_id),
    CONSTRAINT fk_ogpt_og FOREIGN KEY (old_gold_id) REFERENCES old_gold_purchases(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Old Gold Melting Records
-- =============================================
CREATE TABLE old_gold_melting_records (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    old_gold_id     BIGINT         NOT NULL,
    melted_weight   DECIMAL(10,3)  NOT NULL,
    melt_date       DATE           NOT NULL,
    melted_by       VARCHAR(150),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ogmr_og (old_gold_id),
    CONSTRAINT fk_ogmr_og FOREIGN KEY (old_gold_id) REFERENCES old_gold_purchases(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Saving Schemes
-- =============================================
CREATE TABLE schemes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    name            VARCHAR(200)   NOT NULL,
    duration_months INT            NOT NULL,
    monthly_amount  DECIMAL(14,2)  NOT NULL,
    status          ENUM('ACTIVE','CLOSED','PAUSED') DEFAULT 'ACTIVE',
    start_date      DATE           NOT NULL,
    end_date        DATE           NOT NULL,
    bonus_month     BOOLEAN DEFAULT FALSE,
    description     TEXT,
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_sch_store (store_id),
    CONSTRAINT fk_sch_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Scheme Members
-- =============================================
CREATE TABLE scheme_members (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_id       BIGINT         NOT NULL,
    customer_id     BIGINT,
    name            VARCHAR(200)   NOT NULL,
    phone           VARCHAR(15),
    join_date       DATE           NOT NULL,
    status          ENUM('ACTIVE','COMPLETED','DROPPED') DEFAULT 'ACTIVE',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_smem_scheme (scheme_id),
    CONSTRAINT fk_smem_scheme FOREIGN KEY (scheme_id) REFERENCES schemes(id),
    CONSTRAINT fk_smem_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Scheme Payments
-- =============================================
CREATE TABLE scheme_payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_member_id BIGINT        NOT NULL,
    month_number    INT            NOT NULL,
    amount          DECIMAL(14,2)  NOT NULL,
    payment_date    DATE           NOT NULL,
    status          ENUM('PAID','PENDING','LATE') DEFAULT 'PAID',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sp_member (scheme_member_id),
    CONSTRAINT fk_sp_member FOREIGN KEY (scheme_member_id) REFERENCES scheme_members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Organization Settings
-- =============================================
CREATE TABLE org_settings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id          BIGINT         NOT NULL,
    setting_key     VARCHAR(100)   NOT NULL,
    setting_value   TEXT,
    UNIQUE KEY uk_os_org_key (org_id, setting_key),
    CONSTRAINT fk_os_org FOREIGN KEY (org_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Activity Logs
-- =============================================
CREATE TABLE activity_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    user_id         BIGINT,
    user_name       VARCHAR(150),
    action          VARCHAR(200)   NOT NULL,
    detail          TEXT,
    module          VARCHAR(50),
    entity_type     VARCHAR(50),
    entity_id       BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_al_store (store_id),
    KEY idx_al_module (module),
    KEY idx_al_created (created_at),
    CONSTRAINT fk_al_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Add email column to users table for staff updates
-- =============================================
ALTER TABLE users ADD COLUMN email VARCHAR(150) AFTER mobile;
ALTER TABLE users ADD COLUMN salary DECIMAL(14,2) AFTER email;
ALTER TABLE users ADD COLUMN commission DECIMAL(5,2) AFTER salary;
ALTER TABLE users ADD COLUMN sales_target DECIMAL(14,2) AFTER commission;
