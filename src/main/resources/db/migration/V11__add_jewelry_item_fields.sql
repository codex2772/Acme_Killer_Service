-- ============================================
-- V11: Add missing fields to jewelry_items
-- HUID, hallmark cert/date, showcase location, stone weight
-- ============================================

-- HUID — BIS Hallmark Unique ID (legally required on all hallmarked jewelry in India)
ALTER TABLE jewelry_items
    ADD COLUMN huid VARCHAR(50) NULL AFTER barcode;

-- Hallmark certificate number (e.g., HC-2026-12345)
ALTER TABLE jewelry_items
    ADD COLUMN hallmark_cert VARCHAR(100) NULL AFTER huid;

-- Hallmark testing/stamping date
ALTER TABLE jewelry_items
    ADD COLUMN hallmark_date DATE NULL AFTER hallmark_cert;

-- Physical location in store (e.g., "Showcase A - Tray 3")
ALTER TABLE jewelry_items
    ADD COLUMN showcase_location VARCHAR(200) NULL AFTER hallmark_date;

-- Stone weight in grams (weight of stones embedded in the item)
ALTER TABLE jewelry_items
    ADD COLUMN stone_weight DECIMAL(10,3) NULL DEFAULT 0 AFTER net_weight;

-- Index on HUID for quick lookups
CREATE INDEX idx_jewelry_huid ON jewelry_items(huid);

-- Unique constraint: HUID should be unique within a store
-- (NULL values are allowed — not all items have HUID yet)
ALTER TABLE jewelry_items
    ADD CONSTRAINT uq_huid_store UNIQUE (huid, store_id);
