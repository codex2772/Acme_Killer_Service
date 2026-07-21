-- ═══════════════════════════════════════════
-- V15: Invoice PDF (DOCUMENT header) WhatsApp template
-- ═══════════════════════════════════════════
-- Platform-default UTILITY template whose header is a DOCUMENT (the invoice PDF,
-- passed as the send's mediaUrl). The corresponding template MUST be created and
-- APPROVED in WhatsApp Manager as `invoice_document` (UTILITY, language en) with a
-- DOCUMENT header and 4 body variables, or sends fail with Meta #132001/#132000.

INSERT INTO message_templates
    (store_id, code, name, channel, category, meta_template_name, meta_language,
     header_type, body_template, buttons, variables, active)
VALUES
    (NULL, 'INVOICE_DOCUMENT', 'Invoice (PDF)', 'WHATSAPP', 'UTILITY',
     'invoice_document', 'en', 'DOCUMENT',
     'Dear {{1}}, please find attached your invoice {{2}} of Rs.{{3}} from {{4}}. Thank you!',
     NULL,
     '["customerName","invoiceNo","amount","storeName"]', TRUE);
