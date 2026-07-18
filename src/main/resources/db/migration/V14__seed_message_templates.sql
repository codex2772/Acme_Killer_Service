-- ═══════════════════════════════════════════
-- V14: Seed platform-default WhatsApp message templates (store_id NULL)
-- ═══════════════════════════════════════════
-- These are platform defaults shared by all stores. The meta_template_name must
-- match a template approved in each store's Meta WhatsApp account before it can be
-- sent. `hello_world` is Meta's pre-approved sample template — ideal for the M2
-- pilot smoke test against a test number.

INSERT INTO message_templates
    (store_id, code, name, channel, category, meta_template_name, meta_language,
     header_type, body_template, buttons, variables, active)
VALUES
    (NULL, 'HELLO_WORLD', 'Test Hello', 'WHATSAPP', 'UTILITY',
     'hello_world', 'en_US', 'NONE',
     'Hello World', NULL, NULL, TRUE),

    -- Text-only for now (header NONE). Add a DOCUMENT header + invoice-PDF media
    -- once a PDF generation/upload pipeline exists.
    (NULL, 'INVOICE_CONFIRMATION', 'Invoice Confirmation', 'WHATSAPP', 'UTILITY',
     'invoice_confirmation', 'en', 'NONE',
     'Dear {{1}}, your invoice {{2}} of Rs.{{3}} from {{4}} is confirmed. Thank you!',
     NULL,
     '["customerName","invoiceNo","amount","storeName"]', TRUE),

    -- Marketing templates (require customer opt-in; campaign send is gated on it).
    (NULL, 'FESTIVAL_OFFER', 'Festival Offer', 'WHATSAPP', 'MARKETING',
     'festival_offer', 'en', 'NONE',
     '{{1}} Special! Get up to {{2}}% off on gold & diamond jewellery at {{3}}. Visit us today!',
     NULL,
     '["festival","discount","storeName"]', TRUE),

    (NULL, 'CUSTOM_PROMO', 'Custom Promotion', 'WHATSAPP', 'MARKETING',
     'custom_promo', 'en', 'NONE',
     'Dear {{1}}, {{2}} Visit {{3}} to know more.',
     NULL,
     '["customerName","message","storeName"]', TRUE);
