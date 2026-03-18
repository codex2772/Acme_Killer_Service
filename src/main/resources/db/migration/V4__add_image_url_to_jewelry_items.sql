-- =============================================
-- V4__add_image_url_to_jewelry_items.sql
-- Add image_url column to jewelry_items table
-- =============================================

ALTER TABLE jewelry_items ADD COLUMN image_url VARCHAR(500) DEFAULT NULL AFTER barcode;
