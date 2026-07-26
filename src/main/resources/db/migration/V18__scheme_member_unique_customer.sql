-- =============================================
-- V18 — Prevent enrolling the same customer twice in a scheme
-- =============================================
--
-- POST /api/schemes/{id}/members previously accepted a duplicate customer,
-- creating a second member row (with its own payment schedule). This adds a
-- DB-level uniqueness guard on (scheme_id, customer_id).
--
-- customer_id is nullable (manual-entry members have none). MySQL treats NULLs
-- as distinct in a UNIQUE index, so manual members never collide — only
-- customer-linked members are constrained, which is exactly what we want.
--
-- Before adding the constraint we de-duplicate any existing customer-linked
-- duplicates: keep the earliest member (min id), re-point the duplicates'
-- payments onto it (so no payment history is lost), then delete the extras.

-- ---------------------------------------------------------------------------
-- 1. Re-point payments from duplicate members onto the earliest kept member.
-- ---------------------------------------------------------------------------
UPDATE scheme_payments sp
JOIN scheme_members dup ON dup.id = sp.scheme_member_id
JOIN (
    SELECT scheme_id, customer_id, MIN(id) AS keep_id
    FROM scheme_members
    WHERE customer_id IS NOT NULL
    GROUP BY scheme_id, customer_id
    HAVING COUNT(*) > 1
) k ON k.scheme_id = dup.scheme_id AND k.customer_id = dup.customer_id
SET sp.scheme_member_id = k.keep_id
WHERE dup.id <> k.keep_id;

-- ---------------------------------------------------------------------------
-- 2. Delete the duplicate member rows, keeping the earliest per customer.
-- ---------------------------------------------------------------------------
DELETE dup FROM scheme_members dup
JOIN (
    SELECT scheme_id, customer_id, MIN(id) AS keep_id
    FROM scheme_members
    WHERE customer_id IS NOT NULL
    GROUP BY scheme_id, customer_id
    HAVING COUNT(*) > 1
) k ON k.scheme_id = dup.scheme_id AND k.customer_id = dup.customer_id
WHERE dup.id <> k.keep_id;

-- ---------------------------------------------------------------------------
-- 3. Enforce uniqueness of (scheme_id, customer_id) going forward.
-- ---------------------------------------------------------------------------
ALTER TABLE scheme_members
    ADD CONSTRAINT uk_scheme_member_customer UNIQUE (scheme_id, customer_id);
