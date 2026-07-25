-- =============================================
-- V16 — Provision metal types for every store & fix mislinked 22K items
-- =============================================
--
-- Background: metal types are store-scoped, but only the demo stores (1, 2)
-- were ever seeded. Real stores created via onboarding (e.g. Satara=3,
-- Rahimatpur=4) had ZERO metal types. With an empty list the desktop client
-- could not resolve a (metal + purity) match on item create and fell back to
-- metal-type id 1 — the Default Store's 24K — so 22K stock was stored/billed
-- at the 24K rate and cross-linked to another store's metal type.
--
-- This migration:
--   1. Seeds the standard metal-type set for any store that currently has
--      none (covers Satara/Rahimatpur and any other un-provisioned store).
--   2. Re-links the known mislinked 22K items to their own store's 22K Gold.
--
-- Idempotent: the seed only touches stores with zero metal types, and the
-- backfill only re-links items that still point at a different metal type.

-- ---------------------------------------------------------------------------
-- 1. Seed the standard metal-type set for every store that has none.
--    Rates mirror the Store 1 demo seed (V1); Gold 14K added per spec.
-- ---------------------------------------------------------------------------
INSERT INTO metal_types (store_id, name, purity, current_rate, unit)
SELECT s.id, d.name, d.purity, d.current_rate, d.unit
FROM stores s
CROSS JOIN (
    SELECT 'Gold'     AS name, '24K' AS purity, 7500.00 AS current_rate, 'gram' AS unit
    UNION ALL SELECT 'Gold',     '22K', 6900.00, 'gram'
    UNION ALL SELECT 'Gold',     '18K', 5625.00, 'gram'
    UNION ALL SELECT 'Gold',     '14K', 4375.00, 'gram'
    UNION ALL SELECT 'Silver',   '925',   95.00, 'gram'
    UNION ALL SELECT 'Platinum', '950', 3200.00, 'gram'
) d
WHERE NOT EXISTS (
    SELECT 1 FROM metal_types mt WHERE mt.store_id = s.id
);

-- ---------------------------------------------------------------------------
-- 2. Backfill the mislinked 22K items.
--    All four SKUs carry the G22K purity marker (22K Gold) but were linked to
--    metal-type id 1 (Store 1 / 24K). Re-link each to the 22K Gold metal type
--    of its OWN store. The JOIN guarantees a same-store 22K row exists (seeded
--    above), and the id guard makes this a no-op if already correct.
-- ---------------------------------------------------------------------------
UPDATE jewelry_items ji
JOIN metal_types mt
    ON  mt.store_id = ji.store_id
    AND mt.name     = 'Gold'
    AND mt.purity   = '22K'
    AND mt.active   = TRUE
SET ji.metal_type_id = mt.id
WHERE ji.sku IN (
    'NCK-G22K-001Y19G',
    'NCK-G22K-002O8LA',
    'RNG-G22K-001PIDC',
    'NCK-G22K-004KQT7'
)
AND ji.metal_type_id <> mt.id;
