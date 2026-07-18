# JewelERP — WhatsApp Integration: Implementation Plan

> **Version:** 1.0 | **Date:** 2026-07-18
> **Author:** Raviraj Bhosale | **Company:** AuraJewels
> **Scope:** Build plan for WhatsApp messaging — **Meta Cloud API direct**, per-store WABA, WhatsApp-only (SMS/MSG91 deferred)
> **Strategy docs:** `WHATSAPP_INTEGRATION_PLAN.md` (architecture) · `MESSAGING_AND_GST_GUIDE.md` (future SMS/DLT)

This is the technical, file-by-file build plan grounded in the existing codebase (`com.aurajewels.jewel`, Spring Boot 3.4.3, Java 21, MySQL + Flyway, ECS/S3). It follows existing patterns: `BaseEntity` + Lombok entities, `@Service`/`@RequiredArgsConstructor`/`@Slf4j`, Flyway migrations, the Razorpay integration as the external-API + webhook template, and `FeatureModule`/`StoreFeatureModule` for per-store gating.

---

## Table of Contents

1. [Approach & Guardrails](#1-approach--guardrails)
2. [Provider Abstraction (swap-friendly)](#2-provider-abstraction-swap-friendly)
3. [Package & File Layout](#3-package--file-layout)
4. [Data Model — Migration V13](#4-data-model--migration-v13)
5. [Entities](#5-entities)
6. [Repositories](#6-repositories)
7. [DTOs](#7-dtos)
8. [Configuration & Secrets](#8-configuration--secrets)
9. [Service Layer](#9-service-layer)
10. [Controllers & Endpoints](#10-controllers--endpoints)
11. [Auto-Trigger Integration Points](#11-auto-trigger-integration-points)
12. [Webhook Handling](#12-webhook-handling)
13. [Security Config Changes](#13-security-config-changes)
14. [Feature-Flag Gating](#14-feature-flag-gating)
15. [Build Milestones (implementation order)](#15-build-milestones-implementation-order)
16. [Testing Strategy](#16-testing-strategy)
17. [Infra / Deploy Changes](#17-infra--deploy-changes)
18. [Definition of Done](#18-definition-of-done)

---

## 1. Approach & Guardrails

| Guardrail | Decision |
|-----------|----------|
| **Provider now** | Meta WhatsApp **Cloud API direct** (no BSP, no DLT). Wrapped behind a `WhatsAppProvider` interface so MSG91 is a later drop-in. |
| **WABA model** | **Per-store** — each store has its own `store_waba` row (phone_number_id + token). |
| **Channel** | WhatsApp only. Code the channel enum (`WHATSAPP`, `SMS`) now; SMS impl is a no-op stub until MSG91. |
| **Fallback** | For transactional sends, if store not connected → **skip + log `SKIPPED_NO_CHANNEL`** (no SMS yet). Structure supports SMS fallback later. |
| **HTTP client** | Spring `RestClient` (Boot 3.2+, already on classpath) — no new dependency. One `@Bean` for the Meta base client. |
| **Secrets** | Per-store access tokens behind a `WabaTokenStore` interface, referenced by `store_waba.access_token_ref`. **Interim impl shipped:** `LocalEncryptedWabaTokenStore` (AES-GCM, JDK-only, ciphertext stored as the ref — no plaintext token in RDS, no new dependency). **Production:** swap for an AWS Secrets Manager impl (ref = secret name) — one-class change, no callers affected. Platform app secret via `@Value` like Razorpay. |
| **Async** | Sends run on `@Async` executor so invoice creation etc. never blocks on Meta latency. |
| **Idempotency** | Every send writes a `message_logs` row first (status `QUEUED`) → dedupe on `(store_id, customer_id, template, ref_type, ref_id)` for auto-triggers. |
| **Feature gate** | New `WHATSAPP` feature module; `MessagingService` checks `StoreFeatureModule` before sending. |

---

## 2. Provider Abstraction (swap-friendly)

The single most important design choice for "direct now, MSG91 later":

```
interface WhatsAppProvider {
    SendResult sendTemplate(StoreWaba waba, WhatsAppTemplateSend send);
    SendResult sendSessionMessage(StoreWaba waba, String toPhone, WhatsAppMessage msg);
    WabaConnectResult exchangeSignup(String code, Long storeId);   // embedded signup
}
```

- `MetaCloudWhatsAppProvider` — the only impl we build now (calls `graph.facebook.com`).
- `Msg91WhatsAppProvider` — future impl, same interface. Migration = new `@Component` + config flag, zero changes to `MessagingService`.
- Selected by config: `whatsapp.provider=meta` (default) `| msg91`.

`MessagingService` depends only on the interface + `ChannelSelector`. Nothing above the provider knows which vendor is live.

---

## 3. Package & File Layout

All under `src/main/java/com/aurajewels/jewel`, matching existing structure.

```
entity/
  StoreWaba.java
  MessageTemplate.java
  MessageLog.java
  MessageCampaign.java
  (Customer.java              ← add opt-in fields)

repository/
  StoreWabaRepository.java
  MessageTemplateRepository.java
  MessageLogRepository.java
  MessageCampaignRepository.java

dto/messaging/
  SendMessageRequest.java
  BulkMessageRequest.java
  MessageResponse.java
  CampaignResponse.java
  MessageStatsResponse.java
  WabaConnectRequest.java
  WabaStatusResponse.java
  WhatsAppTemplateSend.java      (internal)
  SendResult.java                (internal)

service/messaging/
  WhatsAppProvider.java          (interface)
  MetaCloudWhatsAppProvider.java (impl now)
  MessagingService.java          (orchestration + gate + logging + async)
  ChannelSelector.java
  TemplateResolver.java
  OptInGuard.java
  WabaOnboardingService.java
  MessageCampaignService.java

controller/
  MessageController.java
  WabaController.java
  WhatsAppWebhookController.java   (or extend existing WebhookController)

config/
  WhatsAppConfig.java             (RestClient bean + Meta props)

event/                            (new tiny package, or reuse Spring events)
  InvoiceCreatedEvent.java
  SchemeInstallmentDueEvent.java
  CustomOrderStatusChangedEvent.java
```

---

## 4. Data Model — Migration V13

`src/main/resources/db/migration/V13__whatsapp_messaging.sql` (next free version; V12 is latest). MySQL/InnoDB, matching V8 style.

```sql
-- ═══════════════════════════════════════════
-- V13: WhatsApp Messaging (Meta Cloud API direct, per-store WABA)
-- ═══════════════════════════════════════════

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
    access_token_ref   VARCHAR(255),                -- Secrets Manager key, NOT the token
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
    code                  VARCHAR(60) NOT NULL,      -- INVOICE_CONFIRMATION, BIRTHDAY_WISH...
    name                  VARCHAR(100) NOT NULL,
    channel               ENUM('WHATSAPP','SMS','BOTH') DEFAULT 'WHATSAPP',
    category              ENUM('UTILITY','MARKETING','AUTHENTICATION') DEFAULT 'UTILITY',
    meta_template_name    VARCHAR(120),              -- name registered in Meta
    meta_language         VARCHAR(10) DEFAULT 'en',
    header_type           ENUM('NONE','TEXT','IMAGE','DOCUMENT') DEFAULT 'NONE',
    body_template         TEXT NOT NULL,             -- with {{1}} {{2}} placeholders
    buttons               JSON,                      -- [{type,text,url}]
    variables             JSON,                      -- ordered variable names
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
    status                ENUM('QUEUED','SENT','DELIVERED','READ','FAILED','SKIPPED_NO_CHANNEL',
                               'SKIPPED_NOT_OPTED_IN','SKIPPED_FEATURE_OFF') DEFAULT 'QUEUED',
    failure_reason        VARCHAR(500),
    -- idempotency for auto-triggers
    ref_type              VARCHAR(40),               -- INVOICE / SCHEME_DUE / CUSTOM_ORDER
    ref_id                BIGINT,
    delivered_at          TIMESTAMP NULL,
    read_at               TIMESTAMP NULL,
    sent_by               BIGINT,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    INDEX idx_mlog_store (store_id, created_at),
    INDEX idx_mlog_customer (customer_id),
    INDEX idx_mlog_provider_msg (provider_message_id),
    INDEX idx_mlog_campaign (campaign_id),
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
    audience_filter    JSON,
    variables          JSON,
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
```

> **Note on idempotency key:** `uk_mlog_idem` uses nullable columns; MySQL treats each NULL as distinct, so it only enforces uniqueness for auto-trigger rows where `ref_type`/`ref_id` are set — exactly the intent. Manual/campaign sends (NULL ref) are never blocked.

---

## 5. Entities

Follow the existing convention: standalone `@Entity` with Lombok `@Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor`, `Instant` timestamps via `@CreationTimestamp`/`@UpdateTimestamp` (as in `StoreFeatureModule`), enums as `@Enumerated(EnumType.STRING)`.

| Entity | Key fields | Notes |
|--------|-----------|-------|
| `StoreWaba` | `@OneToOne`/`@ManyToOne Store`, `wabaId`, `phoneNumberId`, `displayName`, `status` (enum), `qualityRating` (enum), `accessTokenRef` | One per store. Token ref only. |
| `MessageTemplate` | `store` (nullable), `code`, `channel`, `category`, `metaTemplateName`, `bodyTemplate`, `buttons` (JSON), `variables` (JSON) | Platform default when `store == null`. |
| `MessageLog` | `store`, `customerId`, `channel`, `recipientPhone`, `status` (enum), `providerMessageId`, `refType`, `refId` | Written before send (QUEUED), updated by webhook. |
| `MessageCampaign` | `store`, `template`, `audienceType` (enum), counters, `status` (enum) | Bulk marketing. |
| `Customer` (edit) | add `whatsappOptIn`, `whatsappOptInSource`, `whatsappOptInAt`, `hasWhatsapp` | Mirror V13 columns. |

Enums to define (nested in entities, like `RazorpayPayment.RazorpayStatus`): `WabaStatus`, `QualityRating`, `MessageChannel`, `MessageStatus`, `TemplateCategory`, `AudienceType`, `CampaignStatus`.

---

## 6. Repositories

Spring Data JPA interfaces (match existing `RazorpayPaymentRepository` style — derived queries).

- `StoreWabaRepository`
  - `Optional<StoreWaba> findByStore_Id(Long storeId)`
  - `Optional<StoreWaba> findByPhoneNumberId(String phoneNumberId)` (webhook routing)
  - `List<StoreWaba> findByStatus(WabaStatus status)`
- `MessageTemplateRepository`
  - `Optional<MessageTemplate> findByStore_IdAndCode(Long storeId, String code)`
  - `Optional<MessageTemplate> findByStoreIsNullAndCode(String code)` (platform default)
  - `List<MessageTemplate> findByStore_Id(Long storeId)`
- `MessageLogRepository`
  - `Optional<MessageLog> findByProviderMessageId(String wamid)` (webhook status update)
  - `boolean existsByStore_IdAndCustomerIdAndRefTypeAndRefId(...)` (idempotency pre-check)
  - `Page<MessageLog> findByStore_IdOrderByCreatedAtDesc(Long storeId, Pageable p)`
  - stats aggregation queries (`@Query` count-by-status per month)
- `MessageCampaignRepository`
  - `Page<MessageCampaign> findByStore_Id(Long storeId, Pageable p)`

---

## 7. DTOs

Under `dto/messaging/`. Records where possible (project uses DTO classes; match whatever `dto/customerapp` uses).

| DTO | Purpose |
|-----|---------|
| `SendMessageRequest` | `{ customerId, templateCode, variables{}, mediaUrl? }` |
| `BulkMessageRequest` | `{ templateCode, audienceType, audienceFilter?, variables{}, mediaUrl?, scheduledAt? }` |
| `MessageResponse` | `{ messageLogId, status, providerMessageId? }` |
| `CampaignResponse` | `{ campaignId, totalRecipients, sent, delivered, read, failed, status }` |
| `MessageStatsResponse` | monthly counts by channel/status |
| `WabaConnectRequest` | `{ code }` (embedded-signup auth code) — Phase 2 |
| `WabaStatusResponse` | `{ status, displayName, displayNumber, qualityRating, messagingTier }` |

---

## 8. Configuration & Secrets

### `application.yaml` (append, mirroring the `razorpay:` block)

```yaml
whatsapp:
  enabled: ${WHATSAPP_ENABLED:false}
  provider: ${WHATSAPP_PROVIDER:meta}      # meta | msg91 (future)
  meta:
    graph-base-url: ${META_GRAPH_URL:https://graph.facebook.com/v21.0}
    app-id: ${META_APP_ID:placeholder}
    app-secret: ${META_APP_SECRET:placeholder}
    webhook-verify-token: ${META_WEBHOOK_VERIFY_TOKEN:placeholder}
  default-channel: WHATSAPP
messaging:
  async-pool-size: ${MESSAGING_ASYNC_POOL:4}
```

### `WhatsAppConfig.java`
- `@Bean RestClient metaRestClient(...)` → base URL `whatsapp.meta.graph-base-url`, default headers, timeouts. (Per-store bearer token is set per-request, not on the bean — tokens differ per store.)
- `@Bean TaskExecutor messagingExecutor(...)` for `@Async`.
- `@ConfigurationProperties("whatsapp")` props class (or `@Value`, matching Razorpay's `@Value` style).

### Secrets
- **Platform-level** (app id/secret, verify token): AWS Secrets Manager → ECS task env, exactly like Razorpay keys. Add to the existing `jewel-erp/prod/*` secret + Terraform task-def secrets block.
- **Per-store access token:** Secrets Manager entry per store, e.g. `jewel-erp/prod/waba/{storeId}`; `store_waba.access_token_ref` holds the key. A small `SecretsTokenResolver` fetches at send time (cache with short TTL). **Never** in RDS/logs.

---

## 9. Service Layer

### `MessagingService` (orchestrator — the public API for the rest of the app)
```
MessageResponse sendTemplated(Long storeId, Long customerId, String templateCode,
                              Map<String,Object> vars, String mediaUrl,
                              String refType, Long refId, Long sentBy);
CampaignResponse sendCampaign(Long storeId, BulkMessageRequest req, Long sentBy);
```
Responsibilities, in order:
1. **Feature gate** — `StoreFeatureModule` has `WHATSAPP` enabled? else log `SKIPPED_FEATURE_OFF`.
2. **Channel select** — `ChannelSelector.pick(store, customer)` → WHATSAPP or none (SMS stub later).
3. **Opt-in** — `OptInGuard`: marketing requires `whatsapp_opt_in`; utility allowed to existing customers (per your compliance stance).
4. **Idempotency** — for auto-triggers, check `uk_mlog_idem` / `existsBy...`; skip duplicates.
5. **Resolve template** — `TemplateResolver` (store override → platform default), fill `{{n}}` from vars.
6. **Persist** `message_logs` row `QUEUED`.
7. **`@Async` send** via `WhatsAppProvider.sendTemplate(waba, send)`.
8. **Update log** with `providerMessageId` + `SENT`/`FAILED`.

### `MetaCloudWhatsAppProvider` (only impl now)
- `sendTemplate`: `POST {graph}/{phone_number_id}/messages` with per-store bearer token, `type=template`, components (header media, body params, buttons).
- `sendSessionMessage`: same endpoint, `type=text/image/document` (used inside 24h window / webhook replies).
- `exchangeSignup`: OAuth code → long-lived token; store ref (Phase 2).
- Parse Meta response → `SendResult{ providerMessageId, ok, error }`.
- Robust error mapping (rate-limit, template-not-approved, invalid-number).

### Supporting services
- `ChannelSelector` — WABA connected + `has_whatsapp != false` → WHATSAPP; else none.
- `TemplateResolver` — resolve + variable substitution + validation (arity match).
- `OptInGuard` — category-based opt-in rules; honors `whatsapp_opt_in`.
- `WabaOnboardingService` — embedded-signup exchange, status sync, token storage (Phase 2). Phase 1: manual admin entry of `phone_number_id` + token for the pilot store.
- `MessageCampaignService` — audience resolution (birthday/anniversary/scheme/all/list), batching within messaging tier, per-recipient send + counters, webhook-driven progress.

---

## 10. Controllers & Endpoints

`MessageController` (`/api/messages`) — JWT-secured, permission-gated like existing controllers.

| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| GET | `/api/messages/templates` | MANAGE_CUSTOMERS | list templates for store |
| POST | `/api/messages/templates` | OWNER/ADMIN | create/override template |
| PUT | `/api/messages/templates/{id}` | OWNER/ADMIN | update |
| POST | `/api/messages/send` | MANAGE_CUSTOMERS | single send |
| POST | `/api/messages/bulk` | OWNER/ADMIN | campaign |
| GET | `/api/messages/campaigns` | VIEW_REPORTS | list |
| GET | `/api/messages/campaigns/{id}` | VIEW_REPORTS | detail + counters |
| GET | `/api/messages/logs` | VIEW_REPORTS | paginated history |
| GET | `/api/messages/stats` | VIEW_REPORTS | monthly usage |

`WabaController` (`/api/waba`) — store WhatsApp connection.

| Method | Path | Permission | Purpose |
|--------|------|-----------|---------|
| GET | `/api/waba/status` | OWNER/ADMIN | this store's connection status |
| POST | `/api/waba/connect` | OWNER/ADMIN | Phase 1: manual creds; Phase 2: embedded-signup code exchange |
| DELETE | `/api/waba/disconnect` | OWNER | disconnect |

`WhatsAppWebhookController` — see §12 (public).

---

## 11. Auto-Trigger Integration Points

Use **Spring `ApplicationEventPublisher`** to keep messaging decoupled from core services (no direct dependency from `InvoiceService` → `MessagingService`).

| Source | Where | Event | Template |
|--------|-------|-------|----------|
| Invoice created | `InvoiceService.createInvoice()` (line ~78) — publish after commit | `InvoiceCreatedEvent{storeId, customerId, invoiceId}` | `INVOICE_CONFIRMATION` (+ invoice PDF media) |
| Scheme installment due | scheduled job (new `@Scheduled` or existing scheduler) | `SchemeInstallmentDueEvent` | `SCHEME_DUE` (+ Razorpay pay button) |
| Custom order status change | `CustomOrder` status update service | `CustomOrderStatusChangedEvent` | `CUSTOM_ORDER_STATUS` |
| Payment received | payment/receipt service | `PaymentReceivedEvent` | `PAYMENT_RECEIPT` |

- A `MessagingEventListener` (`@TransactionalEventListener(phase = AFTER_COMMIT)`, `@Async`) handles each → calls `MessagingService.sendTemplated(...)` with `refType/refId` for idempotency.
- **After-commit** ensures we never message on a rolled-back invoice.
- Invoice PDF: reuse existing S3 invoice generation; pass a signed URL as `mediaUrl`.

---

## 12. Webhook Handling

Meta sends **GET** (verification) and **POST** (events) to one URL. Extend the existing `WebhookController` pattern (unauthenticated, signature-verified, always 200).

`POST/GET /api/webhooks/whatsapp`

- **GET verify:** echo `hub.challenge` when `hub.verify_token == whatsapp.meta.webhook-verify-token`.
- **POST events:** verify `X-Hub-Signature-256` (HMAC-SHA256 with app secret) — like Razorpay's `Utils.verifyWebhookSignature`.
  - **Status updates** (`sent/delivered/read/failed`) → find `message_logs.byProviderMessageId` → update status + timestamps + campaign counters.
  - **Inbound messages** → route by `phone_number_id` → `StoreWaba` → open 24h window; optionally create `CustomerEnquiry` (reuse existing enquiry module) + notify staff.
  - **Account/quality updates** → sync `store_waba.quality_rating` + `messaging_tier`.
- Route by `entry[].changes[].value.metadata.phone_number_id`.
- Always return `200` (log errors), matching Razorpay handling.

---

## 13. Security Config Changes

In `SecurityConfig.java`:
- Permit `**/api/webhooks/whatsapp**` unauthenticated (already the pattern for `/api/webhooks/razorpay`).
- `/api/messages/**` and `/api/waba/**` → authenticated + method-level `@PreAuthorize` per the table above.

---

## 14. Feature-Flag Gating

- `WHATSAPP` seeded into `feature_modules` (V13 step 6).
- Super Admin panel toggles `StoreFeatureModule(WHATSAPP)` per store (existing UI/flow — see `SUPER_ADMIN_PANEL_PLAN.md`).
- `MessagingService` step 1 checks it; disabled → `SKIPPED_FEATURE_OFF`, no send.
- `whatsapp.enabled=false` global kill-switch short-circuits everything (safe default in `application.yaml`).

---

## 15. Build Milestones (implementation order)

Each milestone is independently shippable and testable.

### M1 — Foundation (schema + entities + gating) — ~1 day
- V13 migration, all entities, repositories, `WHATSAPP` feature module.
- `WhatsAppConfig` (RestClient + async + props), `application.yaml` block.
- **Done when:** app boots, migration applies clean, feature module visible in Super Admin.

### M2 — Provider + manual connect + single send — ✅ DONE (code complete)
- `WhatsAppProvider` interface + `MetaCloudWhatsAppProvider` (Graph API template send, body params + media header, error mapping).
- `WabaTokenStore` + `LocalEncryptedWabaTokenStore` (AES-GCM interim; Secrets Manager is the prod drop-in).
- `WabaController` (`/api/waba` connect/status/disconnect) + `WabaOnboardingService` (manual `phone_number_id` + token).
- `MessagingService` (kill-switch → channel → opt-in → idempotency → template → log → send). **Synchronous in M2** for an accurate API response; async-via-events lands in M4.
- `MessageController` `POST /api/messages/send`.
- V14 seeds `HELLO_WORLD` (Meta's pre-approved sample — use for the smoke test) + `INVOICE_CONFIRMATION`.
- Unit tests: phone normalisation, template rendering, token encrypt/resolve. Full suite green.
- **Verify against real DB + Meta:** connect the pilot store, `POST /api/messages/send` with `templateCode=HELLO_WORLD` → real WhatsApp to a test number; `message_logs` row = SENT with a wamid.

### M3 — Webhook (delivery + inbound) — ✅ DONE (code complete)
- `WhatsAppWebhookController` (`/api/webhooks/whatsapp`) — GET verify handshake + POST events; always 200. Path already covered by the `/api/webhooks/**` permitAll rule (no SecurityConfig change).
- `WhatsAppWebhookService` — HMAC-SHA256 (`X-Hub-Signature-256`) verification (skipped when app secret is the dev placeholder); forward-only status advance (QUEUED→SENT→DELIVERED→READ, READ never downgraded, `failed` mapped); inbound routed by `phone_number_id` → store → customer-by-phone → `CustomerEnquiry` (OPEN) + `has_whatsapp=true`; unknown numbers logged only.
- Unit tests: verify handshake, delivered-advances, read-not-downgraded, inbound→enquiry, unknown→no-enquiry.
- **Verify against real DB + Meta:** register the webhook URL in the Meta app; sending shows DELIVERED/READ in `message_logs`; a customer reply creates an enquiry.

### M4 — Auto-trigger: invoice — ✅ DONE (code complete)
- `InvoiceCreatedEvent` (scalar-only payload — no lazy associations touched off-thread) published from `InvoiceService.createInvoice()`.
- `MessagingEventListener` — `@TransactionalEventListener(AFTER_COMMIT)` + `@Async("messagingExecutor")` → `MessagingService.sendTemplated(..., refType=INVOICE, refId=invoiceId)`. Runs only after commit (rolled-back invoice never messages) and off the request thread; exceptions swallowed + logged.
- Idempotency via `ref_type=INVOICE` (pre-check + `uk_mlog_idem`).
- **Text-only for now:** `INVOICE_CONFIRMATION` seeded with `NONE` header — no PDF pipeline exists yet; add a DOCUMENT header + invoice-PDF media when one does.
- Unit tests: kill-switch, duplicate-skip, no-channel-skip, marketing-opt-in-skip, happy-path SENT.
- **Verify against real DB + Meta:** create an invoice for a connected store's customer → exactly one WhatsApp confirmation; creating again for a new invoice sends again, same invoice never twice.

### M5 — Campaigns + opt-in — ✅ DONE (code complete)
- `OptInGuard` (marketing needs consent; utility/auth don't) — now also used inside `MessagingService`.
- `MessageCampaignService` — audience resolution (`ALL_CUSTOMERS`, `SCHEME_MEMBERS`, `CUSTOM_LIST`), opt-in filtering, per-recipient send via `MessagingService` (campaignId threaded onto every log), counters + COMPLETED status. **`BIRTHDAY_TODAY`/`ANNIVERSARY_TODAY` throw "unsupported"** — no customer DOB field exists (would need a schema addition).
- `MessageQueryService` — templates (`findVisibleToStore`), recent logs, current-month stats snapshot, opt-in capture.
- Endpoints: `POST /api/messages/bulk`, `GET /api/messages/{campaigns,campaigns/{id},templates,logs,stats}`, `PUT /api/messages/opt-in`.
- Webhook now bumps campaign `delivered_count`/`read_count` on forward transitions.
- V14 adds `FESTIVAL_OFFER` + `CUSTOM_PROMO` marketing templates.
- Unit tests: opt-in filtering (2 of 3 opted-in → 2 sends), birthday-unsupported.
- **Verify against real DB + Meta:** opt a few customers in, run a `FESTIVAL_OFFER` campaign to `ALL_CUSTOMERS` → only opted-in receive; campaign counters climb as delivery webhooks arrive.

> **Not supported without schema work:** date-based audiences (no `Customer` birthday/anniversary column). Add a V15 column + capture UI if wanted. Tier-throttling/scheduling of large campaigns is also future work (sends are synchronous today).

### M6 — More triggers + polish — ~1–2 days
- Scheme-due (with Razorpay pay button), payment receipt, custom-order status.
- `/api/messages/stats`, logs pagination, quality-rating alert.

### M7 (partial) — Secrets Manager token store — ✅ DONE
- `SecretsManagerWabaTokenStore` (AWS Secrets Manager) alongside the interim `LocalEncryptedWabaTokenStore`, selected by `whatsapp.token-store` (`local` default | `secrets-manager`).
- `SecretsManagerClient` bean is conditional — the default profile needs no AWS. Both stores are `@ConditionalOnProperty`, so exactly one `WabaTokenStore` bean is active.
- `secretsmanager` SDK added (mediated so shared AWS core stays at the existing 2.25.16; only the client differs).
- Verified: full suite green with `local`; app **boots cleanly** with `WABA_TOKEN_STORE=secrets-manager` (beans wire, no ambiguity).
- **To switch prod:** set `WABA_TOKEN_STORE=secrets-manager` + `WABA_SECRET_PREFIX`; grant the task role `secretsmanager:CreateSecret|PutSecretValue|GetSecretValue` on `<prefix>/*`; re-connect each store so tokens move into Secrets Manager.

### (Later) M7 remainder — Embedded signup + MSG91 provider + SMS
- Self-serve `WabaOnboardingService`; `Msg91WhatsAppProvider`; SMS channel + DLT.

**Quick-Start (from strategy doc) maps to:** the `wa.me` button is an independent tiny Desktop-only task that can ship in parallel with M1–M2 for instant value.

---

## 16. Testing Strategy

- **Unit:** `TemplateResolver` (variable substitution/arity), `OptInGuard` (category rules), `ChannelSelector`, webhook signature verification.
- **Provider:** mock `RestClient` / WireMock the Graph API; assert request shape + error mapping.
- **Integration:** `@SpringBootTest` + Testcontainers MySQL (if used elsewhere) — migration applies, send path writes logs, idempotency unique key holds.
- **Webhook:** post sample Meta payloads (status + inbound) → assert log/enquiry state.
- **Manual pilot:** Meta test number → real send/deliver/read; one real store number on unverified tier.
- **Idempotency:** fire `InvoiceCreatedEvent` twice → exactly one send.

---

## 17. Infra / Deploy Changes

- **Secrets Manager:** add `META_APP_ID`, `META_APP_SECRET`, `META_WEBHOOK_VERIFY_TOKEN` to platform secret; per-store `waba/{storeId}` entries created at connect time.
- **Terraform ECS task-def:** add the three META_* secrets + `WHATSAPP_ENABLED`, `WHATSAPP_PROVIDER` env (mirror the Razorpay secrets block).
- **IAM:** ECS task role needs `secretsmanager:GetSecretValue` (+ `CreateSecret`/`PutSecretValue` if the app writes per-store tokens) scoped to `jewel-erp/prod/waba/*`.
- **ALB:** webhook path already covered by existing `/api/webhooks/*` routing; register the URL in Meta App dashboard.
- **Flyway:** V13 runs on deploy (existing `FlywayRepairConfig` handling applies).

---

## 18. Definition of Done

- [ ] V13 migration applied; `WHATSAPP` module toggleable per store.
- [ ] Pilot store connected (manual creds); `/api/messages/send` delivers real WhatsApp.
- [ ] Webhook updates delivery/read status; inbound creates enquiry.
- [ ] Invoice creation auto-sends PDF exactly once (idempotent).
- [ ] Feature flag + global kill-switch both enforced.
- [ ] No token/secret in RDS or logs.
- [ ] Provider swap path proven (interface + config), MSG91 addable without touching `MessagingService`.

---

*Companion to `WHATSAPP_INTEGRATION_PLAN.md`. Author: Raviraj Bhosale | AuraJewels | 2026-07-18*
