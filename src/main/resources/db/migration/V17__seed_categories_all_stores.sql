-- =============================================
-- V17 — Provision categories for every store
-- =============================================
--
-- Twin of V16 (metal types): categories are store-scoped, but only the demo
-- stores (1, 2) were ever seeded. Real stores created via onboarding (e.g.
-- Satara=3, Rahimatpur=4) had ZERO categories. With an empty list the desktop
-- client fell back to category id 1 (Default Store's) on item create; the
-- cross-store guard added in JewelryItemService then correctly rejected it
-- ("Category 1 does not belong to store 4"), blocking all new stock entry.
--
-- This migration seeds the standard category set for any store that currently
-- has none (covers Satara/Rahimatpur and any other un-provisioned store).
-- No item backfill is needed — existing items already have valid categories.
--
-- Idempotent: only touches stores with zero categories.

-- ---------------------------------------------------------------------------
-- Seed the standard category set for every store that has none.
-- Names mirror the Store 1 demo seed (V1) and are chosen so the desktop
-- Add-Inventory dropdown resolves them by name match.
-- ---------------------------------------------------------------------------
INSERT INTO categories (store_id, name, description)
SELECT s.id, d.name, d.description
FROM stores s
CROSS JOIN (
    SELECT 'Rings'     AS name, 'All types of rings'      AS description
    UNION ALL SELECT 'Necklaces', 'Chains and necklaces'
    UNION ALL SELECT 'Earrings',  'Studs, drops, and hoops'
    UNION ALL SELECT 'Bangles',   'Bangles and bracelets'
    UNION ALL SELECT 'Pendants',  'Pendants and lockets'
    UNION ALL SELECT 'Chains',    'Gold and silver chains'
    UNION ALL SELECT 'Anklets',   'Anklets and payal'
) d
WHERE NOT EXISTS (
    SELECT 1 FROM categories c WHERE c.store_id = s.id
);
