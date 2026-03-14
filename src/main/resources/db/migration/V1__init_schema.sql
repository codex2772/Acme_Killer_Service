-- =============================================
-- V1__init_schema.sql
-- JewelERP Initial Database Schema
-- =============================================

-- Base audit columns will be in every table:
-- created_at, updated_at, created_by, updated_by

-- =============================================
-- Categories
-- =============================================
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Metal Types (Gold, Silver, Platinum, etc.)
-- =============================================
CREATE TABLE metal_types (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)    NOT NULL,
    purity      VARCHAR(20)    NOT NULL COMMENT 'e.g., 24K, 22K, 18K, 925',
    current_rate DECIMAL(12,2) NOT NULL COMMENT 'Rate per gram',
    unit        VARCHAR(10)    NOT NULL DEFAULT 'gram',
    active      BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_metal_purity (name, purity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Jewelry Items (Inventory)
-- =============================================
CREATE TABLE jewelry_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku             VARCHAR(50)    NOT NULL,
    name            VARCHAR(200)   NOT NULL,
    description     TEXT,
    category_id     BIGINT         NOT NULL,
    metal_type_id   BIGINT         NOT NULL,
    gross_weight    DECIMAL(10,3)  NOT NULL COMMENT 'Total weight in grams',
    net_weight      DECIMAL(10,3)  NOT NULL COMMENT 'Metal weight in grams',
    making_charges  DECIMAL(12,2)  NOT NULL DEFAULT 0,
    stone_charges   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    other_charges   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    quantity        INT            NOT NULL DEFAULT 1,
    hsn_code        VARCHAR(20)    COMMENT 'HSN code for GST',
    barcode         VARCHAR(100),
    status          ENUM('IN_STOCK', 'SOLD', 'ON_APPROVAL', 'RETURNED', 'DAMAGED') DEFAULT 'IN_STOCK',
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku (sku),
    KEY idx_category (category_id),
    KEY idx_metal_type (metal_type_id),
    KEY idx_status (status),
    CONSTRAINT fk_item_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_item_metal_type FOREIGN KEY (metal_type_id) REFERENCES metal_types(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Customers
-- =============================================
CREATE TABLE customers (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100),
    phone           VARCHAR(15)  NOT NULL,
    email           VARCHAR(150),
    address_line1   VARCHAR(255),
    address_line2   VARCHAR(255),
    city            VARCHAR(100),
    state           VARCHAR(100),
    pincode         VARCHAR(10),
    gstin           VARCHAR(20)  COMMENT 'GST Number if business customer',
    pan             VARCHAR(15),
    active          BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_phone (phone),
    KEY idx_customer_name (first_name, last_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Invoices
-- =============================================
CREATE TABLE invoices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number  VARCHAR(30)    NOT NULL,
    customer_id     BIGINT         NOT NULL,
    invoice_date    DATE           NOT NULL,
    subtotal        DECIMAL(14,2)  NOT NULL DEFAULT 0,
    discount        DECIMAL(14,2)  NOT NULL DEFAULT 0,
    cgst_amount     DECIMAL(14,2)  NOT NULL DEFAULT 0,
    sgst_amount     DECIMAL(14,2)  NOT NULL DEFAULT 0,
    igst_amount     DECIMAL(14,2)  NOT NULL DEFAULT 0,
    total_amount    DECIMAL(14,2)  NOT NULL DEFAULT 0,
    paid_amount     DECIMAL(14,2)  NOT NULL DEFAULT 0,
    payment_status  ENUM('UNPAID', 'PARTIAL', 'PAID') DEFAULT 'UNPAID',
    payment_mode    ENUM('CASH', 'UPI', 'CARD', 'BANK_TRANSFER', 'MIXED'),
    notes           TEXT,
    status          ENUM('DRAFT', 'CONFIRMED', 'CANCELLED') DEFAULT 'DRAFT',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invoice_number (invoice_number),
    KEY idx_customer (customer_id),
    KEY idx_invoice_date (invoice_date),
    KEY idx_status (status),
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Invoice Line Items
-- =============================================
CREATE TABLE invoice_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id      BIGINT         NOT NULL,
    jewelry_item_id BIGINT         NOT NULL,
    quantity        INT            NOT NULL DEFAULT 1,
    metal_rate      DECIMAL(12,2)  NOT NULL COMMENT 'Rate at time of sale',
    metal_value     DECIMAL(14,2)  NOT NULL,
    making_charges  DECIMAL(12,2)  NOT NULL DEFAULT 0,
    stone_charges   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    other_charges   DECIMAL(12,2)  NOT NULL DEFAULT 0,
    discount        DECIMAL(12,2)  NOT NULL DEFAULT 0,
    taxable_amount  DECIMAL(14,2)  NOT NULL,
    cgst_percent    DECIMAL(5,2)   NOT NULL DEFAULT 1.50,
    sgst_percent    DECIMAL(5,2)   NOT NULL DEFAULT 1.50,
    cgst_amount     DECIMAL(12,2)  NOT NULL DEFAULT 0,
    sgst_amount     DECIMAL(12,2)  NOT NULL DEFAULT 0,
    total_amount    DECIMAL(14,2)  NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_invoice (invoice_id),
    KEY idx_jewelry_item (jewelry_item_id),
    CONSTRAINT fk_ii_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_ii_jewelry_item FOREIGN KEY (jewelry_item_id) REFERENCES jewelry_items(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Seed Data
-- =============================================
INSERT INTO categories (name, description) VALUES
    ('Rings', 'All types of rings'),
    ('Necklaces', 'Chains and necklaces'),
    ('Earrings', 'Studs, drops, and hoops'),
    ('Bangles', 'Bangles and bracelets'),
    ('Pendants', 'Pendants and lockets'),
    ('Chains', 'Gold and silver chains'),
    ('Anklets', 'Anklets and payal');

INSERT INTO metal_types (name, purity, current_rate, unit) VALUES
    ('Gold', '24K', 7500.00, 'gram'),
    ('Gold', '22K', 6900.00, 'gram'),
    ('Gold', '18K', 5625.00, 'gram'),
    ('Silver', '925', 95.00, 'gram'),
    ('Silver', '999', 100.00, 'gram'),
    ('Platinum', '950', 3200.00, 'gram');
