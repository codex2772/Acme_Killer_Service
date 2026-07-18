-- ============================================
-- V12: Custom Orders (bespoke / made-to-order)
-- A customer orders a custom piece, pays an advance, the shop crafts it,
-- and on delivery the order is converted into a regular Invoice (the bill).
-- ============================================

-- Custom order header
CREATE TABLE custom_orders (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id              BIGINT         NOT NULL,
    order_number          VARCHAR(30)    NOT NULL,
    customer_id           BIGINT         NOT NULL,
    order_date            DATE           NOT NULL,
    subtotal              DECIMAL(14,2)  NOT NULL DEFAULT 0,
    discount              DECIMAL(14,2)  NOT NULL DEFAULT 0,
    gst_rate              DECIMAL(5,2)   NOT NULL DEFAULT 3.00,
    gst_amount            DECIMAL(14,2)  NOT NULL DEFAULT 0,
    round_off             DECIMAL(10,2)  NOT NULL DEFAULT 0,
    total_amount          DECIMAL(14,2)  NOT NULL DEFAULT 0,
    paid_amount           DECIMAL(14,2)  NOT NULL DEFAULT 0,
    payment_status        ENUM('UNPAID','PARTIAL','PAID') DEFAULT 'UNPAID',
    status                ENUM('PENDING','IN_PROGRESS','READY','DELIVERED','CANCELLED') DEFAULT 'PENDING',
    expected_delivery_date DATE          NULL,
    delivered_date        DATE           NULL,
    goldsmith_name        VARCHAR(150)   NULL,
    assigned_to           BIGINT         NULL,
    design_notes          TEXT,
    reference_image_url   VARCHAR(500)   NULL,
    notes                 TEXT,
    digital_signature     VARCHAR(200),
    converted_invoice_id  BIGINT         NULL,
    active                BOOLEAN DEFAULT TRUE,
    created_by            BIGINT,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_custom_order_number_store (order_number, store_id),
    KEY idx_custom_order_store (store_id),
    KEY idx_custom_order_customer (customer_id),
    KEY idx_custom_order_date (order_date),
    KEY idx_custom_order_status (status),
    CONSTRAINT fk_custom_order_store FOREIGN KEY (store_id) REFERENCES stores(id),
    CONSTRAINT fk_custom_order_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Custom order line items (bespoke; jewelry_item_id optional as the piece may not exist in the catalog yet)
CREATE TABLE custom_order_items (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    custom_order_id     BIGINT         NOT NULL,
    store_id            BIGINT         NOT NULL,
    jewelry_item_id     BIGINT,
    name                VARCHAR(200)   NOT NULL,
    weight              DECIMAL(10,3),
    purity              VARCHAR(20),
    rate                DECIMAL(12,2)  NOT NULL DEFAULT 0,
    making_charge       DECIMAL(12,2)  NOT NULL DEFAULT 0,
    making_charge_type  ENUM('PERCENTAGE','FLAT') DEFAULT 'PERCENTAGE',
    wastage             DECIMAL(5,2)   NOT NULL DEFAULT 0,
    stone_charges       DECIMAL(12,2)  NOT NULL DEFAULT 0,
    amount              DECIMAL(14,2)  NOT NULL DEFAULT 0,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_coi_order (custom_order_id),
    KEY idx_coi_store (store_id),
    CONSTRAINT fk_coi_order FOREIGN KEY (custom_order_id) REFERENCES custom_orders(id),
    CONSTRAINT fk_coi_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Advance / balance payments against a custom order
CREATE TABLE custom_order_payments (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    custom_order_id     BIGINT         NOT NULL,
    store_id            BIGINT         NOT NULL,
    kind                ENUM('ADVANCE','BALANCE') NOT NULL DEFAULT 'ADVANCE',
    mode                VARCHAR(30)    NOT NULL,
    amount              DECIMAL(14,2)  NOT NULL,
    reference           VARCHAR(100),
    payment_date        DATE           NOT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_cop_order (custom_order_id),
    KEY idx_cop_store (store_id),
    CONSTRAINT fk_cop_order FOREIGN KEY (custom_order_id) REFERENCES custom_orders(id),
    CONSTRAINT fk_cop_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Register the CUSTOM_ORDERS feature module (@RequiresModule("CUSTOM_ORDERS"))
-- Guarded so re-running the migration cannot create duplicates.
-- ============================================
INSERT INTO feature_modules (code, name, description, is_core, sort_order)
SELECT 'CUSTOM_ORDERS', 'Custom Orders',
       'Bespoke made-to-order jewellery with advance & delivery tracking', FALSE, 12
WHERE NOT EXISTS (SELECT 1 FROM feature_modules WHERE code = 'CUSTOM_ORDERS');

-- Enable it for all existing stores (backward compatibility)
INSERT INTO store_feature_modules (store_id, module_id, enabled)
SELECT s.id, fm.id, TRUE
FROM stores s
CROSS JOIN feature_modules fm
WHERE fm.code = 'CUSTOM_ORDERS'
  AND NOT EXISTS (
      SELECT 1 FROM store_feature_modules sfm
      WHERE sfm.store_id = s.id AND sfm.module_id = fm.id);

-- ============================================
-- Seed permissions
-- ============================================
INSERT INTO permissions (name, description)
SELECT 'VIEW_CUSTOM_ORDERS', 'View custom orders'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'VIEW_CUSTOM_ORDERS');

INSERT INTO permissions (name, description)
SELECT 'MANAGE_CUSTOM_ORDERS', 'Create/update custom orders and record advances'
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE name = 'MANAGE_CUSTOM_ORDERS');
