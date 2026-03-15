-- =============================================
-- V2__customer_app.sql
-- Customer mobile app: auth, wishlist, enquiries
-- =============================================

-- Add password_hash to customers for mobile app login
ALTER TABLE customers ADD COLUMN password_hash VARCHAR(255) AFTER email;

-- =============================================
-- Customer Wishlists
-- =============================================
CREATE TABLE customer_wishlists (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT NOT NULL,
    jewelry_item_id BIGINT NOT NULL,
    store_id        BIGINT NOT NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wishlist_customer_item (customer_id, jewelry_item_id),
    KEY idx_wl_customer (customer_id),
    KEY idx_wl_store (store_id),
    CONSTRAINT fk_wl_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_wl_item FOREIGN KEY (jewelry_item_id) REFERENCES jewelry_items(id),
    CONSTRAINT fk_wl_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Customer Enquiries
-- =============================================
CREATE TABLE customer_enquiries (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id     BIGINT         NOT NULL,
    jewelry_item_id BIGINT,
    store_id        BIGINT         NOT NULL,
    subject         VARCHAR(200)   NOT NULL,
    message         TEXT           NOT NULL,
    status          ENUM('OPEN','RESPONDED','CLOSED') DEFAULT 'OPEN',
    admin_response  TEXT,
    responded_by    BIGINT,
    responded_at    TIMESTAMP      NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_enq_customer (customer_id),
    KEY idx_enq_store (store_id),
    KEY idx_enq_status (status),
    CONSTRAINT fk_enq_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT fk_enq_item FOREIGN KEY (jewelry_item_id) REFERENCES jewelry_items(id),
    CONSTRAINT fk_enq_store FOREIGN KEY (store_id) REFERENCES stores(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
