-- ═══════════════════════════════════════════
-- V13: WhatsApp Messaging (Meta Cloud API direct, per-store WABA)
-- ═══════════════════════════════════════════
-- JSON-shaped fields are stored as TEXT (serialized JSON) so the schema
-- validates cleanly under spring.jpa.hibernate.ddl-auto=validate.

-- 1. Per-store WhatsApp Business Account connection
CREATE TABLE store_waba (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id           BIGINT NOT NULL,
    waba_id            VARCHAR(64),
    phone_number_id    VARCHAR(64),
    display_number     VARCHAR(20),
    display_name       VARCHAR(200),
    status             ENUM('PENDING','CONNECTED','SUSPENDED','DISCONNECTED') DEFAULT 'PENDING',
    quality_rating     ENUM('HIGH','MEDIUM','LOW','UNKNOWN') DEFAULT 'UNKNOWN',
    messaging_tier     VARCHAR(20),                 -- e.g. TIER_250 / TIER_1K
    access_token_ref   TEXT,                        -- opaque token reference (encrypted token or
                                                    -- secret name), NEVER a plaintext token
    provider           VARCHAR(20) DEFAULT 'META',  -- META | MSG91 (future)
    connected_at       TIMESTAMP NULL,

    active             BOOLEAN DEFAULT TRUE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (store_id) REFERENCES stores(id),
    UNIQUE KEY uk_store_waba_store (store_id),
    INDEX idx_waba_phone (phone_number_id),
    INDEX idx_waba_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Message templates (mapped to Meta-approved templates)
CREATE TABLE message_templates (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id              BIGINT,                    -- NULL = platform default template
    code                  VARCHAR(60) NOT NULL,      -- INVOICE_CONFIRMATION, BIRTHDAY_WISH ...
    name                  VARCHAR(100) NOT NULL,
    channel               ENUM('WHATSAPP','SMS','BOTH') DEFAULT 'WHATSAPP',
    category              ENUM('UTILITY','MARKETING','AUTHENTICATION') DEFAULT 'UTILITY',
    meta_template_name    VARCHAR(120),              -- name registered in Meta
    meta_language         VARCHAR(10) DEFAULT 'en',
    header_type           ENUM('NONE','TEXT','IMAGE','DOCUMENT') DEFAULT 'NONE',
    body_template         TEXT NOT NULL,             -- with {{1}} {{2}} placeholders
    buttons               TEXT,                      -- JSON: [{type,text,url}]
    variables             TEXT,                      -- JSON: ordered variable names

    active                BOOLEAN DEFAULT TRUE,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (store_id) REFERENCES stores(id),
    UNIQUE KEY uk_template_store_code (store_id, code),
    INDEX idx_template_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Message logs (audit + delivery tracking + idempotency)
CREATE TABLE message_logs (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id              BIGINT NOT NULL,
    customer_id           BIGINT,
    campaign_id           BIGINT,
    template_id           BIGINT,
    channel               ENUM('WHATSAPP','SMS') DEFAULT 'WHATSAPP',
    recipient_phone       VARCHAR(20) NOT NULL,
    recipient_name        VARCHAR(200),
    message_body          TEXT,
    media_url             VARCHAR(500),

    -- provider tracking
    provider              VARCHAR(20) DEFAULT 'META',
    provider_message_id   VARCHAR(120),              -- Meta wamid
    conversation_id       VARCHAR(120),
    conversation_category VARCHAR(20),

    status                ENUM('QUEUED','SENT','DELIVERED','READ','FAILED',
                               'SKIPPED_NO_CHANNEL','SKIPPED_NOT_OPTED_IN','SKIPPED_FEATURE_OFF')
                          DEFAULT 'QUEUED',
    failure_reason        VARCHAR(500),

    -- idempotency for auto-triggers
    ref_type              VARCHAR(40),               -- INVOICE / SCHEME_DUE / CUSTOM_ORDER ...
    ref_id                BIGINT,

    delivered_at          TIMESTAMP NULL,
    read_at               TIMESTAMP NULL,
    sent_by               BIGINT,

    active                BOOLEAN DEFAULT TRUE,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    INDEX idx_mlog_store (store_id, created_at),
    INDEX idx_mlog_customer (customer_id),
    INDEX idx_mlog_provider_msg (provider_message_id),
    INDEX idx_mlog_campaign (campaign_id),
    -- NULLs are distinct in MySQL, so this only constrains auto-trigger rows
    -- (where ref_type/ref_id are set); manual + campaign sends are never blocked.
    UNIQUE KEY uk_mlog_idem (store_id, customer_id, template_id, ref_type, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Campaigns (bulk marketing)
CREATE TABLE message_campaigns (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id           BIGINT NOT NULL,
    name               VARCHAR(200) NOT NULL,
    template_id        BIGINT NOT NULL,
    audience_type      ENUM('ALL_CUSTOMERS','BIRTHDAY_TODAY','ANNIVERSARY_TODAY',
                            'SCHEME_MEMBERS','CUSTOM_LIST') NOT NULL,
    audience_filter    TEXT,                          -- JSON
    variables          TEXT,                          -- JSON
    media_url          VARCHAR(500),

    total_recipients   INT DEFAULT 0,
    sent_count         INT DEFAULT 0,
    delivered_count    INT DEFAULT 0,
    read_count         INT DEFAULT 0,
    failed_count       INT DEFAULT 0,

    status             ENUM('DRAFT','QUEUED','SENDING','COMPLETED','FAILED') DEFAULT 'DRAFT',
    scheduled_at       TIMESTAMP NULL,
    completed_at       TIMESTAMP NULL,
    created_by         BIGINT,

    active             BOOLEAN DEFAULT TRUE,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (template_id) REFERENCES message_templates(id),
    INDEX idx_campaign_store (store_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Customer opt-in fields
ALTER TABLE customers ADD COLUMN whatsapp_opt_in BOOLEAN DEFAULT FALSE;
ALTER TABLE customers ADD COLUMN whatsapp_opt_in_source VARCHAR(60);
ALTER TABLE customers ADD COLUMN whatsapp_opt_in_at TIMESTAMP NULL;
ALTER TABLE customers ADD COLUMN has_whatsapp BOOLEAN NULL;   -- learned over time

-- 6. Feature module seed (per-store gating via existing framework)
INSERT INTO feature_modules (code, name, description, is_core, sort_order, active)
VALUES ('WHATSAPP', 'WhatsApp Messaging',
        'Send WhatsApp messages to customers (invoices, reminders, campaigns)',
        FALSE, 100, TRUE);
