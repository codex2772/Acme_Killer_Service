-- =============================================
-- V1__init_schema.sql
-- JewelERP — Complete Database Schema
-- Single migration with all tables, constraints, and seed data
-- =============================================

-- =============================================
-- 1. ORGANIZATIONS & STORES (Multi-tenant core)
-- =============================================
CREATE TABLE organizations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
-- 2. USERS & AUTH
-- =============================================
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id          BIGINT       NOT NULL,
    name            VARCHAR(150) NOT NULL,
    mobile          VARCHAR(15)  NOT NULL,
    email           VARCHAR(150),
    password_hash   VARCHAR(255) NOT NULL,
    role            ENUM('OWNER', 'ADMIN', 'STAFF') NOT NULL,
    salary          DECIMAL(14,2),
    commission      DECIMAL(5,2),
    sales_target    DECIMAL(14,2),
    force_password_change BOOLEAN DEFAULT TRUE,
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_mobile (mobile),
    KEY idx_user_org (org_id),
    CONSTRAINT fk_user_org FOREIGN KEY (org_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_store_access (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    store_id    BIGINT NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_store (user_id, store_id),
    CONSTRAINT fk_usa_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_usa_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    UNIQUE KEY uk_permission_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
-- 3. INVENTORY
-- =============================================
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id    BIGINT       NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_name_store (name, store_id),
    KEY idx_category_store (store_id),
    CONSTRAINT fk_category_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE metal_types (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id     BIGINT         NOT NULL,
    name         VARCHAR(50)    NOT NULL,
    purity       VARCHAR(20)    NOT NULL,
    current_rate DECIMAL(12,2)  NOT NULL,
    unit         VARCHAR(10)    NOT NULL DEFAULT 'gram',
    active       BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_metal_purity_store (name, purity, store_id),
    KEY idx_metal_type_store (store_id),
    CONSTRAINT fk_metal_type_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE jewelry_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    sku             VARCHAR(50)    NOT NULL,
    name            VARCHAR(200)   NOT NULL,
    description     TEXT,
    category_id     BIGINT         NOT NULL,
    metal_type_id   BIGINT         NOT NULL,
    gross_weight    DECIMAL(10,3)  NOT NULL,
    net_weight      DECIMAL(10,3)  NOT NULL,
    making_charges  DECIMAL(12,2)  NOT NULL DEFAULT 0,
    stone_charges   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    other_charges   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    quantity        INT            NOT NULL DEFAULT 1,
    hsn_code        VARCHAR(20),
    barcode         VARCHAR(100),
    status          ENUM('IN_STOCK', 'SOLD', 'ON_APPROVAL', 'RETURNED', 'DAMAGED') DEFAULT 'IN_STOCK',
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku_store (sku, store_id),
    KEY idx_category (category_id),
    KEY idx_metal_type (metal_type_id),
    KEY idx_status (status),
    KEY idx_jewelry_item_store (store_id),
    CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_item_metal_type FOREIGN KEY (metal_type_id) REFERENCES metal_types(id),
    CONSTRAINT fk_jewelry_item_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 4. CUSTOMERS
-- =============================================
CREATE TABLE customers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT       NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100),
    phone           VARCHAR(15)  NOT NULL,
    email           VARCHAR(150),
    address_line1   VARCHAR(255),
    address_line2   VARCHAR(255),
    city            VARCHAR(100),
    state           VARCHAR(100),
    pincode         VARCHAR(10),
    gstin           VARCHAR(20),
    pan             VARCHAR(15),
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phone_store (phone, store_id),
    KEY idx_customer_name (first_name, last_name),
    KEY idx_customer_store (store_id),
    CONSTRAINT fk_customer_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 5. BILLING — Invoices
-- =============================================
CREATE TABLE invoices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id        BIGINT         NOT NULL,
    invoice_type    ENUM('INVOICE','ESTIMATE','CREDIT_NOTE') DEFAULT 'INVOICE',
    invoice_number  VARCHAR(30)    NOT NULL,
    customer_id     BIGINT         NOT NULL,
    invoice_date    DATE           NOT NULL,
    subtotal        DECIMAL(14,2)  NOT NULL DEFAULT 0,
    discount        DECIMAL(14,2)  NOT NULL DEFAULT 0,
    gst_rate        DECIMAL(5,2)   NOT NULL DEFAULT 3.00,
    gst_amount      DECIMAL(14,2)  NOT NULL DEFAULT 0,
    cgst_amount     DECIMAL(14,2)  NOT NULL DEFAULT 0,
    sgst_amount     DECIMAL(14,2)  NOT NULL DEFAULT 0,
    igst_amount     DECIMAL(14,2)  NOT NULL DEFAULT 0,
    round_off       DECIMAL(10,2)  NOT NULL DEFAULT 0,
    old_gold_adjustment DECIMAL(14,2) NOT NULL DEFAULT 0,
    total_amount    DECIMAL(14,2)  NOT NULL DEFAULT 0,
    paid_amount     DECIMAL(14,2)  NOT NULL DEFAULT 0,
    payment_status  ENUM('UNPAID', 'PARTIAL', 'PAID') DEFAULT 'UNPAID',
    payment_mode    ENUM('CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'MIXED'),
    due_date        DATE,
    notes           TEXT,
    digital_signature VARCHAR(200),
    status          ENUM('DRAFT', 'CONFIRMED', 'CANCELLED') DEFAULT 'DRAFT',
    active          BOOLEAN DEFAULT TRUE,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invoice_number_store (invoice_number, store_id),
    KEY idx_customer (customer_id),
    KEY idx_invoice_date (invoice_date),
    KEY idx_invoice_store (store_id),
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_invoice_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invoice_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id      BIGINT         NOT NULL,
    store_id        BIGINT         NOT NULL,
    jewelry_item_id BIGINT,
    quantity        INT            NOT NULL DEFAULT 1,
    metal_rate      DECIMAL(12,2)  NOT NULL DEFAULT 0,
    metal_value     DECIMAL(14,2)  NOT NULL DEFAULT 0,
    making_charges  DECIMAL(12,2)  NOT NULL DEFAULT 0,
    stone_charges   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    other_charges   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    discount        DECIMAL(12,2)  NOT NULL DEFAULT 0,
    taxable_amount  DECIMAL(14,2)  NOT NULL DEFAULT 0,
    cgst_percent    DECIMAL(5,2)   NOT NULL DEFAULT 1.50,
    sgst_percent    DECIMAL(5,2)   NOT NULL DEFAULT 1.50,
    cgst_amount     DECIMAL(12,2)  NOT NULL DEFAULT 0,
    sgst_amount     DECIMAL(12,2)  NOT NULL DEFAULT 0,
    total_amount    DECIMAL(14,2)  NOT NULL DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_invoice (invoice_id),
    KEY idx_invoice_item_store (store_id),
    CONSTRAINT fk_ii_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_invoice_item_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invoice_payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id      BIGINT         NOT NULL,
    store_id        BIGINT         NOT NULL,
    mode            VARCHAR(30)    NOT NULL,
    amount          DECIMAL(14,2)  NOT NULL,
    reference       VARCHAR(100),
    payment_date    DATE           NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ip_invoice (invoice_id),
    KEY idx_ip_store (store_id),
    CONSTRAINT fk_ip_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_ip_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 6. BILLING — Estimates
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
-- 7. BILLING — Credit Notes
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
-- 8. ACCOUNTS — Ledger, Expenses, Cash Register
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
    reference_type  VARCHAR(30),
    created_by      BIGINT,
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_le_store (store_id),
    KEY idx_le_date (entry_date),
    KEY idx_le_type (type),
    CONSTRAINT fk_le_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
-- 9. SUPPLIERS
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

CREATE TABLE supplier_metals (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id     BIGINT         NOT NULL,
    metal           VARCHAR(50)    NOT NULL,
    KEY idx_sm_supplier (supplier_id),
    CONSTRAINT fk_sm_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 10. DAILY RATES & ALERTS
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
-- 11. OLD GOLD
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

CREATE TABLE old_gold_purity_tests (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    old_gold_id     BIGINT         NOT NULL,
    method          VARCHAR(50)    NOT NULL,
    actual_purity   VARCHAR(20),
    purity_percent  DECIMAL(5,2),
    tested_by       VARCHAR(150),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_ogpt_og (old_gold_id),
    CONSTRAINT fk_ogpt_og FOREIGN KEY (old_gold_id) REFERENCES old_gold_purchases(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
-- 12. SAVING SCHEMES
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

CREATE TABLE scheme_payments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    scheme_member_id BIGINT        NOT NULL,
    month_number     INT           NOT NULL,
    amount           DECIMAL(14,2) NOT NULL,
    payment_date     DATE          NOT NULL,
    status           ENUM('PAID','PENDING','LATE') DEFAULT 'PAID',
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sp_member (scheme_member_id),
    CONSTRAINT fk_sp_member FOREIGN KEY (scheme_member_id) REFERENCES scheme_members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- 13. SETTINGS & ACTIVITY LOGS
-- =============================================
CREATE TABLE org_settings (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id          BIGINT         NOT NULL,
    setting_key     VARCHAR(100)   NOT NULL,
    setting_value   TEXT,
    UNIQUE KEY uk_os_org_key (org_id, setting_key),
    CONSTRAINT fk_os_org FOREIGN KEY (org_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
-- 14. SEED DATA — Permissions
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
    ('MANAGE_STAFF',      'Add/remove staff members'),
    ('VIEW_BILLING',      'View invoices, estimates, credit notes'),
    ('MANAGE_BILLING',    'Create/update invoices, estimates, credit notes'),
    ('VIEW_ACCOUNTS',     'View ledger, expenses, suppliers, cash register'),
    ('MANAGE_ACCOUNTS',   'Create/update ledger entries, expenses, suppliers'),
    ('MANAGE_RATES',      'Update daily metal rates'),
    ('MANAGE_OLD_GOLD',   'Record old gold purchases/exchanges'),
    ('MANAGE_SCHEMES',    'Create/manage saving schemes and members');

-- =============================================
-- 15. SEED DATA — Default Org, Store, Owner
-- =============================================
INSERT INTO organizations (id, name) VALUES (1, 'Default Organization');
INSERT INTO stores (id, org_id, name) VALUES (1, 1, 'Default Store');
INSERT INTO stores (id, org_id, name, address, city, state, pincode, phone) VALUES
    (2, 1, 'MG Road Branch', '45 MG Road, Lajpat Nagar', 'New Delhi', 'Delhi', '110024', '9111222333');

-- Owner (password: admin123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change) VALUES
    (1, 1, 'Admin', '9999999999',
     '$2a$10$N5mlLobUQ6gi9j45vgfNx.zvVgLcG.qxCODaXiA1qi7H.vNmHZC7W',
     'OWNER', TRUE);

INSERT INTO user_store_access (user_id, store_id) VALUES (1, 1), (1, 2);

-- =============================================
-- 16. SEED DATA — Sample Users (Admins & Staff)
-- =============================================

-- ADMIN for Default Store (password: branch123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change) VALUES
    (2, 1, 'Vikram Admin', '9876500001',
     '$2a$10$kF7kar3Mt5AL2hMUPCpzbOnCtr0WLlaY2G9HUZLLezvMYCcIr0JAu',
     'ADMIN', FALSE);

-- ADMIN for MG Road Branch (password: branch123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change) VALUES
    (3, 1, 'Priya Branch Admin', '9876500002',
     '$2a$10$kF7kar3Mt5AL2hMUPCpzbOnCtr0WLlaY2G9HUZLLezvMYCcIr0JAu',
     'ADMIN', FALSE);

-- STAFF — Billing (password: staff123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change) VALUES
    (4, 1, 'Amit Billing Staff', '9876500003',
     '$2a$10$4Wxt3dJGoS.SCEsLUm1nheLS6EyfYO0DL6eRD8TNRTI/dnzzzyfkS',
     'STAFF', FALSE);

-- STAFF — Inventory (password: staff123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change) VALUES
    (5, 1, 'Sunita Inventory Staff', '9876500004',
     '$2a$10$kCkwznf.VsJpL2yLJjVO5OcLA0QyEt8JXGKc6Vx8TVDS9rKILwkSC',
     'STAFF', FALSE);

-- STAFF — View-Only (password: staff123)
INSERT INTO users (id, org_id, name, mobile, password_hash, role, force_password_change) VALUES
    (6, 1, 'Rohan View-Only Staff', '9876500005',
     '$2a$10$4Wxt3dJGoS.SCEsLUm1nheLS6EyfYO0DL6eRD8TNRTI/dnzzzyfkS',
     'STAFF', FALSE);

-- Store access
INSERT INTO user_store_access (user_id, store_id) VALUES
    (2, 1), (3, 2), (4, 1), (5, 1), (6, 1), (6, 2);

-- Staff permissions
INSERT INTO user_permissions (user_id, store_id, permission_id) VALUES
    (4, 1, 1), (4, 1, 3), (4, 1, 5), (4, 1, 6), (4, 1, 7),
    (5, 1, 1), (5, 1, 2), (5, 1, 3), (5, 1, 4),
    (6, 1, 1), (6, 1, 3), (6, 1, 6), (6, 1, 8),
    (6, 2, 1);

-- =============================================
-- 17. SEED DATA — Sample Inventory
-- =============================================

-- Categories for Store 1
INSERT INTO categories (store_id, name, description) VALUES
    (1, 'Rings', 'All types of rings'),
    (1, 'Necklaces', 'Chains and necklaces'),
    (1, 'Earrings', 'Studs, drops, and hoops'),
    (1, 'Bangles', 'Bangles and bracelets'),
    (1, 'Pendants', 'Pendants and lockets'),
    (1, 'Chains', 'Gold and silver chains'),
    (1, 'Anklets', 'Anklets and payal');

-- Categories for Store 2
INSERT INTO categories (store_id, name, description) VALUES
    (2, 'Rings', 'All types of rings'),
    (2, 'Necklaces', 'Chains and necklaces'),
    (2, 'Earrings', 'Studs, drops, and hoops');

-- Metal Types for Store 1
INSERT INTO metal_types (store_id, name, purity, current_rate, unit) VALUES
    (1, 'Gold', '24K', 7500.00, 'gram'),
    (1, 'Gold', '22K', 6900.00, 'gram'),
    (1, 'Gold', '18K', 5625.00, 'gram'),
    (1, 'Silver', '925', 95.00, 'gram'),
    (1, 'Silver', '999', 100.00, 'gram'),
    (1, 'Platinum', '950', 3200.00, 'gram');

-- Metal Types for Store 2
INSERT INTO metal_types (store_id, name, purity, current_rate, unit) VALUES
    (2, 'Gold', '22K', 7100.00, 'gram'),
    (2, 'Gold', '18K', 5800.00, 'gram'),
    (2, 'Silver', '925', 98.00, 'gram');

-- Sample Customers for Store 1
INSERT INTO customers (store_id, first_name, last_name, phone, email, address_line1, city, state, pincode) VALUES
    (1, 'Rajesh', 'Sharma', '9876543210', 'rajesh.sharma@email.com', '12 MG Road', 'Mumbai', 'Maharashtra', '400001'),
    (1, 'Priya', 'Patel', '9876543211', 'priya.patel@email.com', '45 Ring Road', 'Ahmedabad', 'Gujarat', '380001'),
    (1, 'Amit', 'Verma', '9876543212', 'amit.verma@email.com', '78 Sector 15', 'Noida', 'Uttar Pradesh', '201301'),
    (1, 'Sunita', 'Reddy', '9876543213', 'sunita.reddy@email.com', '23 Jubilee Hills', 'Hyderabad', 'Telangana', '500033'),
    (1, 'Vikram', 'Singh', '9876543214', 'vikram.singh@email.com', '56 Civil Lines', 'Jaipur', 'Rajasthan', '302001');

-- Sample Jewelry Items for Store 1 (category_id and metal_type_id reference store 1 entries)
INSERT INTO jewelry_items (store_id, sku, name, description, category_id, metal_type_id, gross_weight, net_weight, making_charges, stone_charges, hsn_code, quantity, status) VALUES
    (1, 'RNG-G22-001', 'Classic Gold Band', 'Simple 22K gold band ring', 1, 2, 5.200, 5.000, 1500.00, 0.00, '7113', 3, 'IN_STOCK'),
    (1, 'RNG-G22-002', 'Diamond Solitaire Ring', '22K gold ring with single diamond', 1, 2, 4.800, 4.200, 2500.00, 15000.00, '7113', 2, 'IN_STOCK'),
    (1, 'NCK-G22-001', 'Traditional Haar', '22K gold traditional necklace', 2, 2, 35.500, 34.000, 12000.00, 0.00, '7113', 1, 'IN_STOCK'),
    (1, 'NCK-G22-002', 'Choker Necklace', '22K gold choker with temple design', 2, 2, 25.200, 24.000, 9000.00, 5000.00, '7113', 2, 'IN_STOCK'),
    (1, 'EAR-G22-001', 'Jhumka Earrings', '22K gold traditional jhumkas', 3, 2, 12.300, 11.800, 4500.00, 0.00, '7113', 5, 'IN_STOCK'),
    (1, 'BNG-G22-001', 'Plain Gold Bangle (pair)', '22K gold plain bangles set of 2', 4, 2, 22.000, 21.500, 6000.00, 0.00, '7113', 3, 'IN_STOCK'),
    (1, 'RNG-S92-001', 'Silver Toe Ring', '925 sterling silver toe ring', 1, 4, 2.500, 2.400, 200.00, 0.00, '7113', 10, 'IN_STOCK'),
    (1, 'NCK-S92-001', 'Silver Chain', '925 sterling silver chain 20 inch', 6, 4, 15.000, 14.800, 500.00, 0.00, '7113', 8, 'IN_STOCK'),
    (1, 'RNG-P95-001', 'Platinum Wedding Band', '950 platinum wedding band', 1, 6, 8.000, 7.800, 5000.00, 0.00, '7113', 2, 'IN_STOCK');
