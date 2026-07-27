-- =============================================
-- V19 — Rebuild invoice accounting from source of truth
-- =============================================
--
-- Invoice ledger entries were split between the desktop and the backend,
-- producing two historical defects:
--   1. Create-time double payment-credit: the backend posted a "Sale" CR AND
--      the desktop posted a "Payment" CR for the same initial payment.
--   2. Cancelled invoices were never reversed: their sale/payment entries
--      stayed active, so cancelled sales still counted as revenue and cash.
--
-- The create-time desktop CRs cannot be reliably distinguished from legitimate
-- record-payment CRs by note/category, so rather than surgically deleting
-- duplicates we rebuild every invoice's accounting from the invoices and
-- invoice_payments tables (the source of truth). This fixes both defects at
-- once and matches exactly what the backend now posts going forward:
--   CONFIRMED  -> receivable DR (total) + revenue CR (total) + receipt CR per payment
--   CANCELLED  -> nothing active (net zero)
--   DRAFT      -> nothing (no committed accounting)
--
-- Only invoice-referenced rows are touched; manual ledger entries, expenses,
-- old-gold and credit-note entries are left untouched.

-- ---------------------------------------------------------------------------
-- 1. Deactivate all existing invoice ledger entries (duplicates, stale sale
--    CRs, and any prior reversals) so we can regenerate a clean set.
-- ---------------------------------------------------------------------------
UPDATE ledger_entries
SET active = FALSE
WHERE reference_type IN ('INVOICE', 'INVOICE_REVERSAL');

-- ---------------------------------------------------------------------------
-- 2a. Receivable DR (invoice total) for each confirmed invoice.
-- ---------------------------------------------------------------------------
INSERT INTO ledger_entries
    (store_id, entry_date, party, type, amount, mode, note, category,
     reference_id, reference_type, created_by, active)
SELECT i.store_id,
       i.invoice_date,
       CONCAT(c.first_name,
              CASE WHEN c.last_name IS NULL OR c.last_name = '' THEN ''
                   ELSE CONCAT(' ', c.last_name) END),
       'DR',
       i.total_amount,
       COALESCE(i.payment_mode, 'CASH'),
       CONCAT('Invoice ', i.invoice_number, ' — receivable'),
       'Receivable',
       i.invoice_number,
       'INVOICE',
       i.created_by,
       TRUE
FROM invoices i
JOIN customers c ON c.id = i.customer_id
WHERE i.status = 'CONFIRMED' AND i.active = TRUE;

-- ---------------------------------------------------------------------------
-- 2b. Revenue CR (invoice total) for each confirmed invoice.
-- ---------------------------------------------------------------------------
INSERT INTO ledger_entries
    (store_id, entry_date, party, type, amount, mode, note, category,
     reference_id, reference_type, created_by, active)
SELECT i.store_id,
       i.invoice_date,
       CONCAT(c.first_name,
              CASE WHEN c.last_name IS NULL OR c.last_name = '' THEN ''
                   ELSE CONCAT(' ', c.last_name) END),
       'CR',
       i.total_amount,
       COALESCE(i.payment_mode, 'CASH'),
       CONCAT('Sale — ', i.invoice_number),
       'Sales',
       i.invoice_number,
       'INVOICE',
       i.created_by,
       TRUE
FROM invoices i
JOIN customers c ON c.id = i.customer_id
WHERE i.status = 'CONFIRMED' AND i.active = TRUE;

-- ---------------------------------------------------------------------------
-- 2c. Receipt CR for every recorded payment of a confirmed invoice.
-- ---------------------------------------------------------------------------
INSERT INTO ledger_entries
    (store_id, entry_date, party, type, amount, mode, note, category,
     reference_id, reference_type, created_by, active)
SELECT p.store_id,
       p.payment_date,
       CONCAT(c.first_name,
              CASE WHEN c.last_name IS NULL OR c.last_name = '' THEN ''
                   ELSE CONCAT(' ', c.last_name) END),
       'CR',
       p.amount,
       p.mode,
       CONCAT('Payment for ', i.invoice_number),
       'Receipts',
       i.invoice_number,
       'INVOICE',
       i.created_by,
       TRUE
FROM invoice_payments p
JOIN invoices i ON i.id = p.invoice_id
JOIN customers c ON c.id = i.customer_id
WHERE i.status = 'CONFIRMED' AND i.active = TRUE;
