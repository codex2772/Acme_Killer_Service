-- ============================================
-- V10: Add stone_details table for multi-stone support
-- Each jewelry item can have 0..N stones
-- ============================================

CREATE TABLE stone_details (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    jewelry_item_id BIGINT          NOT NULL,
    stone_type      VARCHAR(50)     NOT NULL COMMENT 'Diamond, Ruby, Emerald, Sapphire, Kundan/Polki, Pearl, Other',
    carat           DECIMAL(8,2)    NULL     COMMENT 'Stone weight in carats',
    cut             VARCHAR(50)     NULL     COMMENT 'Brilliant Round, Princess, Oval, Cushion, Pear, Marquise, Cabochon, Uncut',
    clarity         VARCHAR(20)     NULL     COMMENT 'FL, IF, VVS1, VVS2, VS1, VS2, SI, Eye Clean',
    color           VARCHAR(30)     NULL     COMMENT 'e.g., D, E-F, Pigeon Blood, etc.',
    certification   VARCHAR(20)     NULL     COMMENT 'GIA, IGI, AGS, HRD, None',
    created_at      TIMESTAMP       NULL     DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NULL     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_stone_jewelry_item (jewelry_item_id),
    INDEX idx_stone_type (stone_type),

    CONSTRAINT fk_stone_jewelry_item
        FOREIGN KEY (jewelry_item_id) REFERENCES jewelry_items(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
