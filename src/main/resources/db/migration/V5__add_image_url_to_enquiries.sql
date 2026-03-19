-- =============================================
-- V5__add_image_url_to_enquiries.sql
-- Add image_url column to customer_enquiries table
-- =============================================

ALTER TABLE customer_enquiries ADD COLUMN image_url VARCHAR(500) DEFAULT NULL AFTER message;
