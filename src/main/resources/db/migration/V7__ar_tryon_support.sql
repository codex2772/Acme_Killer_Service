-- ═══════════════════════════════════════════
-- V7: AR Virtual Try-On Support
-- ═══════════════════════════════════════════

-- 1. Add AR fields to jewelry_items (safe: ignore if already exists)
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'jewelry_items' AND COLUMN_NAME = 'ar_enabled');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE jewelry_items ADD COLUMN ar_enabled BOOLEAN DEFAULT FALSE',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'jewelry_items' AND COLUMN_NAME = 'ar_type');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE jewelry_items ADD COLUMN ar_type ENUM(''NECKLACE'',''CHOKER'',''EARRING'',''RING'',''BANGLE'',''BRACELET'',''MAANG_TIKKA'',''NOSE_RING'',''ANKLET'',''PENDANT'',''CHAIN'') DEFAULT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. AR Assets table (overlay images + positioning config)
CREATE TABLE IF NOT EXISTS ar_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    jewelry_item_id BIGINT NOT NULL,
    store_id BIGINT NOT NULL,
    ar_type ENUM(
        'NECKLACE','CHOKER','EARRING','RING','BANGLE','BRACELET',
        'MAANG_TIKKA','NOSE_RING','ANKLET','PENDANT','CHAIN'
    ) NOT NULL,
    overlay_url VARCHAR(500),
    model_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    anchor_config JSON,
    status ENUM('PENDING','READY','FAILED','DISABLED') DEFAULT 'PENDING',
    source ENUM('MANUAL_UPLOAD','AUTO_GENERATED','PHOTO_PROCESSED') DEFAULT 'MANUAL_UPLOAD',
    file_size_bytes BIGINT DEFAULT 0,
    width INT,
    height INT,
    FOREIGN KEY (jewelry_item_id) REFERENCES jewelry_items(id) ON DELETE CASCADE,
    FOREIGN KEY (store_id) REFERENCES stores(id),
    UNIQUE KEY uk_item_ar (jewelry_item_id)
);

-- 3. AR Sessions table (analytics — track try-on usage)
CREATE TABLE IF NOT EXISTS ar_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    store_id BIGINT NOT NULL,
    user_id BIGINT,
    jewelry_item_id BIGINT NOT NULL,
    duration_seconds INT DEFAULT 0,
    screenshot_taken BOOLEAN DEFAULT FALSE,
    led_to_invoice BOOLEAN DEFAULT FALSE,
    invoice_id BIGINT,
    device_type VARCHAR(50),
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (jewelry_item_id) REFERENCES jewelry_items(id),
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);

-- 4. Add AR_TRYON module to feature_modules
INSERT IGNORE INTO feature_modules (code, name, description, is_core, sort_order)
VALUES ('AR_TRYON', 'AR Virtual Try-On', 'Virtual jewelry try-on using webcam overlay', FALSE, 12);

-- 5. Enable AR for all existing stores (default ON)
INSERT INTO store_feature_modules (store_id, module_id, enabled)
SELECT s.id, fm.id, TRUE
FROM stores s CROSS JOIN feature_modules fm
WHERE fm.name = 'AR_TRYON'
ON DUPLICATE KEY UPDATE enabled = TRUE;

-- 6. Indexes for faster lookups (safe: check before creating)
SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ar_assets' AND INDEX_NAME = 'idx_ar_assets_store_status');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_ar_assets_store_status ON ar_assets (store_id, status)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ar_sessions' AND INDEX_NAME = 'idx_ar_sessions_store_created');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX idx_ar_sessions_store_created ON ar_sessions (store_id, created_at)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
