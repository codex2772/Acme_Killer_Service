# JewelERP — WhatsApp Go-Live Runbook (Meta Setup → Production)

> **Version:** 1.0 | **Date:** 2026-07-18
> **Audience:** Operator setting up the Meta account and taking WhatsApp live
> **Prereq reading:** `WHATSAPP_IMPLEMENTATION_PLAN.md` (what the backend does)

The backend (M1–M5) is built and DB-verified. Everything below is the **Meta-side + config work** to make real messages flow. There are two phases:

- **Phase A — Pilot on a test number** (a few hours, no business verification). Proves the end-to-end path.
- **Phase B — Go live on a store's real number** (adds business verification + a permanent token).

Nothing here needs new code. You'll set a handful of env vars and call two endpoints we already built.

---

## What you need before starting

| Item | Notes |
|------|-------|
| A Facebook account | Personal login used to access Business tools |
| A Meta Business Portfolio | Created at business.facebook.com (free) |
| A phone number for the store | **Must NOT be active on the WhatsApp / WhatsApp Business app.** Can receive SMS or a call for the one-time verification. A landline works (voice OTP). |
| Business documents (Phase B) | GST certificate / shop license / utility bill — for Meta Business Verification |
| The deployed backend URL | Public HTTPS, e.g. `https://api.yourdomain.com` — needed for the webhook |

---

## Phase A — Pilot on a Meta test number (do this first)

### A1. Create a Meta app
1. Go to **developers.facebook.com** → log in → **My Apps** → **Create App**.
2. App type: **Business**. Give it a name (e.g. "JewelERP Messaging").
3. Link it to your Business Portfolio when asked.

### A2. Add the WhatsApp product
1. In the app dashboard → **Add product** → **WhatsApp** → **Set up**.
2. Meta automatically provisions:
   - A **test phone number** (you send *from* this).
   - A **temporary access token** (valid ~24 hours).
   - A **Phone number ID** and a **WhatsApp Business Account (WABA) ID**.
3. Note these three values from the **API Setup** panel:
   - `Phone number ID`
   - `WhatsApp Business Account ID`
   - `Temporary access token`

### A3. Add your own phone as a test recipient
- On the same API Setup panel, under **To**, add **your own mobile number** (up to 5 recipients allowed without going live). Verify it with the code Meta sends.
- This lets you receive real messages from the test number immediately.

### A4. Collect the app secret + pick a verify token
1. App dashboard → **App settings → Basic**:
   - Copy **App ID** → this is `META_APP_ID`.
   - Copy **App secret** → this is `META_APP_SECRET`.
2. **Invent** a random string for the webhook handshake → this is `META_WEBHOOK_VERIFY_TOKEN` (e.g. a UUID). You choose it; Meta just echoes it back.

### A5. Configure the backend
Set these environment variables on the running backend (ECS task / local `.env`), then restart:

```
WHATSAPP_ENABLED=true
WHATSAPP_PROVIDER=meta
META_APP_ID=<from A4>
META_APP_SECRET=<from A4>
META_WEBHOOK_VERIFY_TOKEN=<your random string from A4>
WABA_TOKEN_ENCRYPTION_KEY=<a long random string, keep stable>
```

> `WABA_TOKEN_ENCRYPTION_KEY` encrypts store access tokens at rest. Set it once and **don't change it** or previously stored tokens can't be decrypted.

### A6. Register the webhook with Meta
1. App dashboard → **WhatsApp → Configuration → Webhook** → **Edit**.
2. **Callback URL:** `https://<your-backend>/api/webhooks/whatsapp`
3. **Verify token:** the exact `META_WEBHOOK_VERIFY_TOKEN` you set in A5.
4. Click **Verify and save** — Meta calls your GET endpoint; it must echo the challenge (we tested this works).
5. Under **Webhook fields**, click **Manage** and **Subscribe** to at least **`messages`** (covers delivery/read statuses *and* inbound messages).

### A7. Enable the WHATSAPP module for the pilot store
The store must have the `WHATSAPP` feature module enabled (Super Admin panel, or directly). Without it the API endpoints return 403.

### A8. Connect the store to the test number
Call the connect endpoint (as a store OWNER/ADMIN user with a valid JWT):

```
POST /api/waba/connect
{
  "phoneNumberId": "<Phone number ID from A2>",
  "wabaId":        "<WABA ID from A2>",
  "displayName":   "Pilot Store",
  "displayNumber": "<the test number>",
  "accessToken":   "<temporary access token from A2>"
}
```

Then confirm:
```
GET /api/waba/status   →   { "connected": true, "status": "CONNECTED", ... }
```

### A9. Send the smoke-test message
`hello_world` is a Meta pre-approved template and is already seeded as template code `HELLO_WORLD`.

```
POST /api/messages/send
{ "customerId": <a customer whose phone = your A3 test recipient>, "templateCode": "HELLO_WORLD" }
```

Expected: **your phone receives "Hello World"**, and:
```
GET /api/messages/logs   →   the row shows status SENT then DELIVERED/READ
```
Reply to the message from your phone → check it creates an **enquiry** for that customer (inbound routing).

✅ **Phase A done:** the full pipeline works — send, delivery receipts, inbound.

> **Token expiry note:** the Phase-A token lasts ~24h. When it expires, sends start failing; re-run A8 with a fresh temporary token, or move to Phase B's permanent token.

---

## Phase B — Go live on a store's real number

### B1. Add the store's real phone number
1. **WhatsApp Manager** (business.facebook.com/wa/manage) → your WABA → **Phone numbers** → **Add phone number**.
2. Enter the store's business number + display name (this is what customers see).
3. Verify via **SMS or voice call** (the number must not be on the WhatsApp app).
4. You'll get a **new Phone number ID** for this real number.

### B2. Complete Meta Business Verification
1. **business.facebook.com → Settings → Business Info / Security Center → Start Verification**.
2. Submit business details + a document (GST certificate, shop license, or utility bill in the business name).
3. Approval typically takes **1–3 business days** (can be longer).
4. Verification raises your messaging limit from **250 → 1,000 → 10,000 → 100,000** unique customers/24h as volume + quality grow, and is required for a **verified display name (green check)** application.

> You can keep piloting on the test number while verification is pending.

### B3. Create a permanent access token (System User)
Temporary tokens expire; production needs a non-expiring **System User token**:
1. **business.facebook.com → Settings → Users → System Users → Add** (name it e.g. "jewelerp-messaging", role: Admin or Employee).
2. **Assign assets** → add your **App** and your **WABA** with full control.
3. **Generate new token** → select the app → grant scopes **`whatsapp_business_messaging`** and **`whatsapp_business_management`** → set expiry to **Never**.
4. Copy the token (shown once).

### B4. Register real templates
For anything beyond `hello_world`, create + get templates approved:
1. **WhatsApp Manager → Message Templates → Create Template**.
2. Match the seeded codes so the backend finds them (or update the DB rows). Names + languages must match:
   - `invoice_confirmation` (UTILITY, en)
   - `festival_offer` (MARKETING, en)
   - `custom_promo` (MARKETING, en)
3. Meta review is usually **minutes to a few hours**. Categories: **UTILITY** (invoice/order), **MARKETING** (offers), **AUTHENTICATION** (OTP).
4. Body placeholders `{{1}} {{2}}…` must line up with the seeded variable order.

> The seeded rows are platform defaults. If a store needs its own wording, create a store-scoped template row with the same `code` — the backend prefers the store's over the default.

### B5. Reconnect the store with the real number + permanent token
```
POST /api/waba/connect
{
  "phoneNumberId": "<real number's Phone number ID from B1>",
  "wabaId":        "<WABA ID>",
  "displayName":   "Prakash Jewellers",
  "displayNumber":"<store number>",
  "accessToken":   "<permanent System User token from B3>"
}
```
(Re-connecting overwrites the pilot values for that store.)

### B6. Go-live checklist
- [ ] Business verified (or accepted the 250/day cap for a soft launch)
- [ ] Real number added + verified, display name set
- [ ] Permanent token connected via `/api/waba/connect`
- [ ] `invoice_confirmation` template **approved** in Meta (else auto-invoice sends fail)
- [ ] Webhook URL is the **production** HTTPS URL, `messages` field subscribed
- [ ] `WHATSAPP_ENABLED=true` in prod; `WABA_TOKEN_ENCRYPTION_KEY` set + stable
- [ ] Opt-in captured for customers before any **marketing** send (`PUT /api/messages/opt-in`)
- [ ] Create a real invoice → customer receives the confirmation (auto-trigger works)

---

## Production hardening (do soon after go-live)

| Item | Why |
|------|-----|
| **Swap token store to AWS Secrets Manager** | Interim store encrypts tokens in the DB; Secrets Manager is the intended prod home. One-class change (`WabaTokenStore` impl) — see plan §2/§8. Needs the `secretsmanager` SDK dep + IAM `GetSecretValue`. |
| **Rotate `META_APP_SECRET` handling** | Keep app secret + tokens only in Secrets Manager / task secrets, never in code or plain env files in git. |
| **Apply for green-tick display name** | WhatsApp Manager → phone number → verified name request (needs business verification + notability). Improves customer trust. |
| **Monitor quality rating** | WhatsApp Manager shows per-number quality (High/Med/Low). Low rating throttles sending — the webhook already syncs it to `store_waba.quality_rating`. |
| **Watch messaging tier** | New numbers start at 250/day; don't blast un-engaged lists or the tier won't grow. |

---

## Compliance reminders (India + Meta)

- **WhatsApp needs NO DLT** — DLT is SMS-only. Ignore DLT for this channel.
- **Marketing requires opt-in.** Only send `MARKETING` templates to customers who opted in; the backend already blocks the rest (`SKIPPED_NOT_OPTED_IN`).
- **24-hour window:** free-form replies are only allowed within 24h of the customer's last message. Business-initiated messages must use approved templates (the backend only sends templates).
- **Give an opt-out** (e.g. "Reply STOP") in marketing messages and honor it via `PUT /api/messages/opt-in {optIn:false}`.

---

## Quick reference — the values you'll collect

| Value | Where from | Used as |
|-------|-----------|---------|
| App ID | App → Settings → Basic | `META_APP_ID` |
| App secret | App → Settings → Basic | `META_APP_SECRET` |
| Verify token | You invent it | `META_WEBHOOK_VERIFY_TOKEN` |
| Phone number ID | WhatsApp → API Setup (test) / WhatsApp Manager (real) | `/api/waba/connect` body |
| WABA ID | WhatsApp → API Setup | `/api/waba/connect` body |
| Access token | Temp (API Setup) → then System User (permanent) | `/api/waba/connect` body |
| Encryption key | You invent it, keep stable | `WABA_TOKEN_ENCRYPTION_KEY` |

---

## Troubleshooting

| Symptom | Likely cause |
|---------|-------------|
| `/api/messages/send` returns 403 | `WHATSAPP` module not enabled for the store, or user lacks `MANAGE_CUSTOMERS` |
| Log status `SKIPPED_NO_CHANNEL` | Store not connected / token wrong — re-run `/api/waba/connect` |
| Log status `SKIPPED_FEATURE_OFF` | `WHATSAPP_ENABLED` is false |
| Log status `SKIPPED_NOT_OPTED_IN` | Marketing template to a non-opted-in customer (expected) |
| Log status `FAILED` with template error | Template name/language not approved in Meta, or variable count mismatch |
| Webhook "Verify and save" fails | Callback URL not public HTTPS, or verify token mismatch |
| Send works but no DELIVERED/READ | `messages` webhook field not subscribed |
| Sends suddenly fail after a day (pilot) | Temporary token expired — move to permanent System User token (B3) |

---

*Companion to `WHATSAPP_IMPLEMENTATION_PLAN.md`. Author: Raviraj Bhosale | AuraJewels | 2026-07-18*
