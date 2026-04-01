-- ═══════════════════════════════════════════
-- V8: Razorpay Payment Gateway Integration
-- ═══════════════════════════════════════════

-- 1. Razorpay payment tracking table
CREATE TABLE razorpay_payments (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Link to scheme payment (filled when payment succeeds)
    scheme_payment_id     BIGINT,
    scheme_member_id      BIGINT NOT NULL,

    -- Razorpay identifiers
    razorpay_order_id     VARCHAR(50) NOT NULL UNIQUE,
    razorpay_payment_id   VARCHAR(50),
    razorpay_signature    VARCHAR(255),

    -- Payment details
    amount_paise          BIGINT NOT NULL,
    currency              VARCHAR(5) DEFAULT 'INR',
    month_number          INT NOT NULL,

    -- Status tracking
    status                ENUM('CREATED','AUTHORIZED','CAPTURED','PAID','FAILED','REFUNDED')
                          DEFAULT 'CREATED',
    failure_reason        VARCHAR(500),

    -- Metadata
    customer_id           BIGINT NOT NULL,
    store_id              BIGINT NOT NULL,
    payment_method        VARCHAR(30),

    -- Audit
    active                BOOLEAN DEFAULT TRUE,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (scheme_payment_id) REFERENCES scheme_payments(id),
    FOREIGN KEY (scheme_member_id) REFERENCES scheme_members(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (store_id) REFERENCES stores(id),

    INDEX idx_rp_order (razorpay_order_id),
    INDEX idx_rp_payment (razorpay_payment_id),
    INDEX idx_rp_member (scheme_member_id),
    INDEX idx_rp_customer (customer_id),
    INDEX idx_rp_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Add razorpay reference columns to scheme_payments
ALTER TABLE scheme_payments ADD COLUMN razorpay_payment_id VARCHAR(50);
ALTER TABLE scheme_payments ADD COLUMN payment_method VARCHAR(30) DEFAULT 'CASH';
