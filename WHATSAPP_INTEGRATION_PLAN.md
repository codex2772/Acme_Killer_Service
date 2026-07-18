# JewelERP — WhatsApp-First Messaging Integration Plan

> **Version:** 1.0 | **Date:** 2026-07-18
> **Author:** Raviraj Bhosale | **Company:** AuraJewels
> **Module:** Customer Messaging — WhatsApp (primary) + SMS (fallback/OTP)
> **Companion doc:** `MESSAGING_AND_GST_GUIDE.md` (SMS + DLT + GST detail)

---

## 0. Decisions locked for this plan

| Decision | Choice | Why |
|----------|--------|-----|
| **Scope right now** | **WhatsApp only** — SMS/MSG91/DLT deferred | DLT registration is slow; WhatsApp needs **no DLT** and can ship now |
| **Immediate path** | **Phase 0.5 Quick-Start** (`wa.me` link → Meta Cloud API direct) | Live in days without a BSP or business verification; see [§0.5](#05-quick-start-ship-whatsapp-now) |
| **Primary channel** | **WhatsApp first**, SMS as fallback/OTP (later) | Jewelry needs rich media (invoice PDFs, product photos, catalogs); ~90% open rate vs ~20% for SMS |
| **WABA ownership** | **Per-store WABA** (each store's own number) | Real store branding + green tick; quality rating isolated per store; store owns its customer relationship |
| **Provider (BSP)** | **Meta Cloud API direct now → MSG91 later** | Direct = fastest to ship, no BSP fees; MSG91 removes per-store ops load at scale |
| **SMS DLT model** | **Deferred** — platform-level when we do it (existing guide) | Not on the critical path; WhatsApp doesn't depend on it |

> **Key correction:** **DLT is an SMS-only requirement.** WhatsApp has never needed DLT. The slow registration in `MESSAGING_AND_GST_GUIDE.md` gates *only* the SMS channel — so WhatsApp can go live independently, right now.

The asymmetry is deliberate: **SMS = platform-level** (branding capped anyway, deferred), **WhatsApp = per-store** (full business name + logo + green tick shown to customer, shipping now).

---

## 0.5 Quick-Start: Ship WhatsApp NOW

Goal: get real WhatsApp messages flowing **in days, not weeks** — no DLT, no BSP, no business verification on the critical path. Two steps, ship them in order.

### Step 1 — `wa.me` "Send on WhatsApp" button (this week, zero setup)

A button in the Desktop app that opens WhatsApp with a pre-filled message to the customer. Owner taps **Send**.

```
"Send on WhatsApp"  →  opens  https://wa.me/<customer_number>?text=<prefilled invoice text + PDF link>
```

| Aspect | Detail |
|--------|--------|
| **Setup** | None — no API, no Meta account, no verification, **no cost** |
| **Sends from** | Owner's **personal/business WhatsApp** app |
| **Good for** | Invoice/receipt sharing, enquiry replies — immediately |
| **Limits** | Semi-manual (one tap each), no automation, no bulk, no delivery receipts |
| **Effort** | A few hours (URL builder + button in invoice/customer screens) |

This is the instant win. It buys time while Step 2 is built.

### Step 2 — WhatsApp Cloud API **direct from Meta** (≈1–2 days work, no BSP)

Automated sending — invoice PDFs, buttons, bulk within limits — connecting straight to Meta, skipping MSG91 for now.

| Aspect | Detail |
|--------|--------|
| **Instant test number** | Meta gives a free test number that messages up to 5 numbers — build & demo today |
| **Production (unverified)** | Add one real store number; message **~250 unique customers/24h**, up to 2 numbers, **before** business verification |
| **Templates** | Meta approval in **minutes–hours** (vs 24–48h DLT) |
| **DLT / BSP** | **None** — no DLT, no BSP fees |
| **Automation** | Full — PDFs, buttons, bulk (within the daily limit) |
| **Later** | Business verification raises limits (1K→10K→100K/day) + unlocks green tick |

### What Quick-Start does NOT change

- **Per-store WABA** still holds — Meta's embedded signup exists in the direct API; you just run it yourself instead of MSG91 running it.
- **Architecture is future-proof** — everything routes through one channel-agnostic `MessagingService` + a provider client. Migrating **Cloud API direct → MSG91 BSP** later is a **one-client swap**, not a rewrite (see [§6](#6-architecture)).

### The one tradeoff to accept

Going direct means **you** own the per-store WABA/token/webhook plumbing. Fine for 1–5 pilot stores; that operational load is exactly what MSG91 removes at scale. So: **direct for speed now, MSG91 for scale later.**

### Quick-Start sequence

| # | Action | Time | Blocker |
|---|--------|------|---------|
| 1 | `wa.me` button in Desktop (invoice + customer screens) | Hours | None |
| 2 | Meta for Developers app + free test number | ~1 hr | Facebook account |
| 3 | Approve first templates (invoice, receipt) | Minutes–hours | Step 2 |
| 4 | Connect **1 pilot store's** real number (unverified tier) | ~1 hr | Number not on WhatsApp app |
| 5 | Wire `WhatsAppClient` (direct) into `MessagingService` + webhook | 1–2 days | Steps 2–4 |
| 6 | Pilot: auto invoice PDF over WhatsApp | — | Step 5 |
| — | *(parallel, no rush)* Meta business verification → higher limits | Days–weeks | — |
| — | *(later)* Migrate to MSG91 BSP + add SMS/DLT | — | — |

---

## Table of Contents

1. [Why WhatsApp-First for Jewelry](#1-why-whatsapp-first-for-jewelry)
2. [How WhatsApp Business API Works (BSP model)](#2-how-whatsapp-business-api-works-bsp-model)
3. [Per-Store WABA Onboarding (Embedded Signup)](#3-per-store-waba-onboarding-embedded-signup)
4. [Message Types & The 24-Hour Window](#4-message-types--the-24-hour-window)
5. [Template Strategy](#5-template-strategy)
6. [Architecture](#6-architecture)
7. [Data Model](#7-data-model)
8. [Send Flows](#8-send-flows)
9. [Inbound & Webhooks](#9-inbound--webhooks)
10. [Opt-In & Compliance](#10-opt-in--compliance)
11. [Cost Model](#11-cost-model)
12. [Configuration & Secrets](#12-configuration--secrets)
13. [Phased Rollout](#13-phased-rollout)
14. [Risks & Mitigations](#14-risks--mitigations)
15. [Open Questions](#15-open-questions)

---

## 1. Why WhatsApp-First for Jewelry

| Capability | SMS | WhatsApp | Why it matters for jewelry |
|------------|-----|----------|----------------------------|
| Send invoice **PDF** | ❌ | ✅ | Customer keeps the actual GST invoice |
| Send **product photos / catalog** | ❌ | ✅ | New arrivals, bespoke design previews |
| **Buttons / quick replies** | ❌ | ✅ | "Confirm pickup", "Pay now", "View design" |
| **Pay link** (Razorpay) inline | Link only | Rich CTA button | Ties into existing Razorpay integration |
| Order/custom-order status updates | Plain text | Rich, threaded | Bespoke made-to-order flow (recent feature) |
| Open / read rate | ~15–20% | ~90%+ | Higher engagement, real read receipts |
| Two-way conversation | ❌ | ✅ | Customer replies, enquiries captured |

**SMS keeps two jobs:** (1) **OTP** for customer-app login, (2) **fallback** when a customer has no WhatsApp or a WhatsApp send fails.

---

## 2. How WhatsApp Business API Works (BSP model)

You cannot connect to Meta's WhatsApp API directly as easily as a large enterprise; you go through a **BSP (Business Solution Provider)**. **MSG91 is our BSP.**

```
Store's WhatsApp number  ──(managed by)──►  MSG91 (BSP)  ──►  Meta WhatsApp Cloud API  ──►  Customer's WhatsApp
        ▲                                       ▲
        │ connected once via                    │ JewelERP backend calls
        │ Embedded Signup                       │ MSG91 WhatsApp API
        └───────────────────────────────────────┘
```

Key concepts:
- **WABA** = WhatsApp Business Account (per store).
- **Phone number** = the store's business number, hosted on the Cloud API (must NOT be active on the personal WhatsApp app).
- **Display name + green tick** = shown to the customer; verified by Meta.
- **Quality rating** = Meta scores each number (High/Medium/Low) based on blocks/reports; low rating throttles sending. **Per-store isolation protects good stores from bad ones.**
- **Messaging limits (tiers)** = new numbers start at 1,000 unique customers/24h, auto-scale to 10K → 100K → unlimited as quality + volume grow.

---

## 3. Per-Store WABA Onboarding (Embedded Signup)

Each store connects **their own** WhatsApp number once. This is the WhatsApp equivalent of the store's Razorpay onboarding.

### One-time flow (in the Desktop/Electron app)

```
Store owner: Settings → Messaging → "Connect WhatsApp"
      │
      ▼
1. JewelERP opens Meta Embedded Signup popup (hosted via MSG91's onboarding)
      │
      ▼
2. Owner logs in to their Facebook Business account (or creates one)
      │
      ▼
3. Owner selects/creates WABA + enters business WhatsApp number
      │
      ▼
4. Number verified via OTP (SMS/call to that number)
      │
      ▼
5. Meta returns WABA ID + phone_number_id + access token → captured by MSG91 → stored per store in JewelERP
      │
      ▼
6. Store status = CONNECTED. Backend can now send from this store's number.
```

### What each store needs before onboarding
- A phone number **not currently registered on the WhatsApp/WhatsApp Business app** (or willing to delete it from the app).
- A **Facebook Business Manager** account (created during signup if absent).
- Business verification documents (GST/PAN/shop proof) — Meta may require this for higher limits and the green tick.

### Onboarding tiers you can offer (productize this)
| Tier | What store does | What you do |
|------|-----------------|-------------|
| **Self-serve** | Runs embedded signup themselves | Provide the button + guide |
| **Concierge (paid add-on)** | Sends you docs + a spare SIM | You run signup + verification for them |

> **Fallback for Phase 1:** Until a store connects, route that store's messages over **SMS only**. No WhatsApp = graceful degradation, not a blocker.

---

## 4. Message Types & The 24-Hour Window

WhatsApp has two sending modes — this drives cost and what's allowed:

| Mode | When usable | Content | Cost |
|------|-------------|---------|------|
| **Template message** | Anytime (business-initiated) | Pre-approved template only | Billed per conversation, by category |
| **Session / free-form** | Only within **24h** of the customer's last message to you | Any content (free text, media) | Free / cheap (inside open window) |

Practical rules:
- To **start** a conversation (invoice alert, reminder, offer) → you MUST use an **approved template**.
- Once the customer **replies**, a 24h **service window** opens — you can send free-form (e.g., answer their enquiry, send extra photos) at no/low cost.
- Templates are categorized by Meta: **Utility** (transactional), **Marketing** (promotional), **Authentication** (OTP). Category sets the price and the rules.

---

## 5. Template Strategy

Register these WhatsApp templates in MSG91 → Meta. Approval is usually minutes–hours (much faster than DLT's 24–48h).

### Utility templates (transactional — auto-triggered)
| Template | Trigger | Media | Buttons |
|----------|---------|-------|---------|
| Invoice confirmation | Invoice created | **Invoice PDF** | "View", "Pay now" (if unpaid) |
| Payment receipt | Payment recorded | Receipt PDF | — |
| Scheme installment due | Scheduled reminder | — | "Pay now" (Razorpay link) |
| Custom order status | Order stage change | Design photo (optional) | "View details" |
| Repair/enquiry response | Staff replies to enquiry | Optional image | "Call store" |

### Marketing templates (promotional — owner-triggered, opt-in required)
| Template | Trigger | Media |
|----------|---------|-------|
| Birthday wish + offer | Auto (birthday = today) | Optional banner |
| Anniversary wish + offer | Auto | Optional banner |
| Festival campaign | Owner campaign (Akshaya Tritiya, Diwali, Dhanteras) | Product image / catalog |
| New arrivals | Owner campaign | Product images / catalog |
| Gold rate update | Scheduled/owner | Optional rate card image |
| Custom broadcast | Owner types message, picks audience | Optional |

### Authentication template
| Template | Trigger |
|----------|---------|
| OTP | Login — **but default OTP stays on SMS**; WhatsApp OTP optional later |

> **Variables** work like DLT: `{{1}}`, `{{2}}` … resolved per recipient (customer name, invoice no, amount, store name). Store name is a variable, but because each store sends from **its own number**, branding is already native.

---

## 6. Architecture

```
┌────────────────────┐     ┌─────────────────────────────┐     ┌──────────────────┐
│ Desktop (Electron) │     │      Spring Boot Backend     │     │      MSG91       │
│ Admin App (Flutter)│────►│                             │────►│  WhatsApp (BSP)  │
│ Customer (Flutter) │     │  MessagingService (channel-  │     │  SMS Gateway     │
│                    │     │   agnostic router)          │     │  OTP             │
│ "Send campaign"    │     │   ├─ WhatsAppClient         │     └────────┬─────────┘
│ "Connect WhatsApp" │     │   ├─ Msg91SmsClient         │              │ webhooks
└────────────────────┘     │   ├─ TemplateResolver       │◄─────────────┘ (delivery +
                           │   ├─ ChannelSelector        │                 inbound msgs)
                           │   └─ OptInGuard             │
                           └──────────────┬──────────────┘
                                          │
                     ┌────────────────────┴─────────────────────┐
                     │ RDS: store_waba, message_templates,       │
                     │ message_logs, message_campaigns,          │
                     │ customer opt-in fields                    │
                     └───────────────────────────────────────────┘
```

**Channel router logic (`ChannelSelector`):**
1. Is store's WhatsApp `CONNECTED` **and** customer opted-in **and** customer has WhatsApp? → **WhatsApp**.
2. Else → **SMS** (if transactional or SMS opt-in ok).
3. On WhatsApp send failure → **auto-fallback to SMS** for transactional messages.

This keeps one `MessagingService` API; callers (InvoiceService, SchemeService, campaign UI) never care which channel wins.

---

## 7. Data Model

New/changed tables (extends the `message_*` tables already specified in `MESSAGING_AND_GST_GUIDE.md`).

### New: `store_waba` (per-store WhatsApp connection)
| Column | Purpose |
|--------|---------|
| `store_id` | FK → stores |
| `waba_id` | Meta WhatsApp Business Account ID |
| `phone_number_id` | Meta phone number ID (the send identity) |
| `display_number` | Human-readable business number |
| `display_name` | Verified business name shown to customers |
| `status` | `PENDING`, `CONNECTED`, `SUSPENDED`, `DISCONNECTED` |
| `quality_rating` | `HIGH`/`MEDIUM`/`LOW` (synced from webhook) |
| `messaging_tier` | Current daily unique-customer limit |
| `access_token_ref` | Reference to token in Secrets Manager (never store raw token in DB) |
| `connected_at`, `updated_at` | Audit |

### Extend `message_templates`
- Add `whatsapp_template_name`, `whatsapp_category` (`UTILITY`/`MARKETING`/`AUTHENTICATION`), `header_type` (`NONE`/`IMAGE`/`DOCUMENT`), `buttons` (JSON).

### Extend `message_logs`
- Add `conversation_id`, `conversation_category`, `channel_used` (final channel after fallback), `fell_back` (bool), `media_url`.

### Extend `customers`
- `whatsapp_opt_in` (bool), `whatsapp_opt_in_source`, `whatsapp_opt_in_at`, `has_whatsapp` (nullable — learned over time), keep existing `sms_opt_out`.

---

## 8. Send Flows

### Flow A — Auto invoice over WhatsApp (with SMS fallback)
```
Staff creates invoice
   │
   ▼
InvoiceService → MessagingService.sendInvoice(customer, invoice)
   │
   ├─ ChannelSelector: store WABA CONNECTED + customer opted-in + has WhatsApp?
   │        │
   │        ├─ YES → WhatsAppClient: send "invoice_confirmation" template
   │        │         header = invoice PDF, buttons = [View, Pay now]
   │        │         └─ success → log (channel=WHATSAPP)
   │        │         └─ failure → fall back ▼
   │        │
   │        └─ NO / fallback → Msg91SmsClient: DLT invoice template
   │                            └─ log (channel=SMS, fell_back=true)
   ▼
Customer receives invoice PDF on WhatsApp from "Prakash Jewellers" ✔ green tick
```

### Flow B — Marketing campaign (owner-triggered)
```
Owner → Marketing → New Campaign
   ├─ pick template (e.g., "festival_offer")
   ├─ pick audience (ALL / BIRTHDAY_TODAY / SCHEME_MEMBERS / CUSTOM_LIST)
   ├─ attach image/catalog, set variables, preview
   ▼
MessagingService.sendCampaign()
   ├─ OptInGuard filters to WhatsApp-opted-in customers only (marketing!)
   ├─ respect messaging tier (batch within daily unique-customer limit)
   ├─ send template per recipient, log each
   ▼
Campaign dashboard: sent / delivered / read / failed  (from webhooks)
```

### Flow C — Scheme reminder with pay button
```
Scheduler (daily) → find installments due
   ▼
MessagingService.sendSchemeReminder()
   ├─ WhatsApp "scheme_due" template
   ├─ button "Pay now" → Razorpay payment link (existing integration)
   ▼
Customer taps → pays in-thread
```

---

## 9. Inbound & Webhooks

MSG91 → your backend webhook receives:
- **Delivery/read status** → update `message_logs` (`SENT`/`DELIVERED`/`READ`/`FAILED`).
- **Inbound customer messages** → opens/extends the 24h service window; route to the correct **store** (by `phone_number_id`), optionally create an **enquiry** (ties into existing enquiry module), notify staff.
- **Quality/tier updates** → sync `store_waba.quality_rating` + `messaging_tier`; alert owner if rating drops to LOW.

Webhook endpoints (public, signature-verified):
- `POST /api/webhooks/whatsapp/events` (status + inbound + account updates)
- `POST /api/webhooks/msg91/delivery` (SMS delivery — from existing guide)

---

## 10. Opt-In & Compliance

**WhatsApp (Meta):**
- **Opt-in is mandatory** before sending templates. Capture consent explicitly:
  - Checkbox at billing / customer registration.
  - "Message us to get updates" (inbound = implicit opt-in for the window; still record explicit for marketing).
- Record `whatsapp_opt_in_source` + timestamp — **not just a boolean** (needed if Meta audits).
- Marketing templates → only to opted-in customers; provide easy opt-out ("Reply STOP"), honor it.
- Respect **quality rating**; don't blast unengaged lists — it tanks the store's number.

**SMS (DLT):** unchanged — platform-level DLT, promotional 9am–9pm, transactional bypasses DND, `sms_opt_out` honored. See existing guide §15.

**Data:** WhatsApp media (invoice PDFs) already served from your S3 setup — ensure signed/expiring URLs.

---

## 11. Cost Model

**WhatsApp** — Meta bills per **24-hour conversation**, by category (indicative 2026 India rates; confirm live in MSG91):

| Conversation category | Rough cost | Notes |
|-----------------------|-----------|-------|
| Utility (invoice, receipt, order) | Low | **Free when sent as reply inside an open service window**; Meta trending toward per-message utility pricing |
| Authentication (OTP) | Low | We mostly keep OTP on SMS |
| Marketing (offers) | Higher (~₹0.7–1.0+) | The cost driver — gate behind opt-in |
| Service (customer-initiated) | Free tier + low | First replies inside window often free |

**SMS:** ₹0.11–0.15 per SMS (existing guide §12).

**Blended per store (~500 customers):**
| Usage | Volume/mo | Indicative cost |
|-------|-----------|-----------------|
| Invoice + receipt (WhatsApp utility) | ~300 | low |
| Scheme + order updates (utility) | ~150 | low |
| 1–2 marketing campaigns (WhatsApp) | ~600–1000 | the main cost |
| OTP + SMS fallback | ~200 | ₹25–30 |
| **Total** | | **~₹200–500/mo** |

> **Make this a billable line / plan tier**, not an absorbed cost. Options: (a) bundle N conversations per plan then meter overage, (b) markup on MSG91 cost, (c) marketing campaigns as paid add-on. **Recommendation:** utility bundled into subscription, marketing metered/add-on.

**Scale note:** MSG91 adds a per-message markup. At 50+ active stores, re-evaluate migrating **WhatsApp** to **360dialog** (flat monthly + Meta at-cost) while keeping SMS on MSG91. The channel-router architecture makes this a client swap, not a rewrite.

---

## 12. Configuration & Secrets

```yaml
messaging:
  default-channel: WHATSAPP
  fallback-to-sms: true
whatsapp:
  provider: msg91
  base-url: https://control.msg91.com/api/v5/whatsapp
  enabled: ${WHATSAPP_ENABLED:false}
msg91:
  auth-key: ${MSG91_AUTH_KEY}         # shared SMS + WhatsApp
  sender-id: ${MSG91_SENDER_ID:JEWELP}
  webhook-secret: ${MSG91_WEBHOOK_SECRET}
```

- **Per-store WhatsApp access tokens** → AWS Secrets Manager, referenced by `store_waba.access_token_ref`. Never in RDS or app config.
- Platform-level `MSG91_AUTH_KEY` stays in the existing `jewel-erp/prod/db-credentials` secret pattern.
- Feature flag: add `WHATSAPP` to `feature_modules` so Super Admin toggles it per store (mirrors `MESSAGING`).

---

## 13. Phased Rollout

| Phase | Scope | Gate / lead time |
|-------|-------|------------------|
| **0.5 Quick-Start (NOW)** | `wa.me` button; **Meta Cloud API direct**; 1 pilot store on unverified tier; auto invoice PDF. See [§0.5](#05-quick-start-ship-whatsapp-now) | **None** — ships in days |
| **0. Foundations** | *(parallel/later)* Meta Business verification (raises limits); MSG91 WhatsApp BSP account; confirm live pricing | Meta verification: days–weeks |
| **1. Core infra** | `store_waba` table + channel router + `WhatsAppClient`; embedded-signup button in Desktop; **1 pilot store connected** | Phase 0 |
| **2. Utility messaging** | Invoice PDF + payment receipt + scheme reminder + custom-order status over WhatsApp, **SMS fallback**; delivery/read webhooks | Templates approved (hours) |
| **3. Inbound + service window** | Inbound routing → enquiry module; 24h free-form replies; staff notification | Phase 2 |
| **4. Marketing engine** | Campaign builder UI; audience segments; opt-in management; festival/gold-rate/new-arrival broadcasts | Phase 2 |
| **5. Optimization** | Cost dashboard per store; quality-rating alerts; evaluate 360dialog for WhatsApp at scale | Volume-driven |

> **Parallelize:** Meta business verification (Phase 0) and SMS/DLT (existing guide) both have long lead times — kick both off before writing feature code.

---

## 14. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Meta business verification delays | Blocks WhatsApp go-live | Start Phase 0 immediately; SMS-only fallback keeps product usable |
| Store's number already on WhatsApp app | Can't connect | Onboarding checklist; concierge tier with spare SIM |
| Store quality rating drops (spammy owner) | Throttled/blocked number | Per-store isolation; opt-in enforcement; unengaged-list warnings; rate alerts |
| Meta pricing model shifts (per-message utility) | Cost surprise | Own `message_logs` tracking; utility bundled, marketing metered |
| Token expiry per store | Silent send failures | Token refresh job; `store_waba.status` health check + owner alert |
| Customer has no WhatsApp | Message not delivered | Auto SMS fallback for transactional |
| BSP lock-in | Migration pain | Channel-router abstraction; keep 360dialog as documented exit for WhatsApp |

---

## 15. Open Questions

1. **OTP channel:** keep OTP on SMS (recommended, universal) or offer WhatsApp OTP where connected?
2. **Marketing monetization:** bundle N conversations/plan + overage, flat markup, or paid add-on? (Recommendation: utility bundled, marketing metered.)
3. **Concierge onboarding:** offer paid WhatsApp setup for non-technical store owners? (Likely yes — most jewelers won't self-serve embedded signup.)
4. **Gold-rate broadcasts:** daily automated (high engagement, higher cost) vs weekly? Per-store toggle?
5. **Green tick:** pursue verified display name per store (better trust) or defer until a store requests it?

---

## Summary

| Topic | Decision |
|-------|----------|
| Scope now | **WhatsApp only** — SMS/MSG91/DLT deferred (DLT ≠ WhatsApp) |
| Immediate path | **Phase 0.5**: `wa.me` button → Meta Cloud API direct, live in days |
| Primary channel | **WhatsApp**; SMS = fallback + OTP (later) |
| WABA model | **Per-store** via Meta embedded signup |
| Provider | **Meta Cloud API direct now → MSG91 later** (one-client swap) |
| Branding | Native per-store (own number + name + green tick) |
| Architecture | Single channel-agnostic `MessagingService` + router + auto-fallback |
| Compliance | Meta template approval + explicit opt-in (WhatsApp) — **no DLT needed** |
| Cost | Direct: Meta conversation cost only, no BSP markup; ~₹200–500/store/mo |
| Biggest lead-time gate | **None for Quick-Start**; Meta business verification only raises limits later |

---

*Companion to `MESSAGING_AND_GST_GUIDE.md`. Author: Raviraj Bhosale | AuraJewels | 2026-07-18*
