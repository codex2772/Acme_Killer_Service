# JewelERP — Super Admin Panel (Platform Management)

> **Date:** March 19, 2026
> **Purpose:** Internal admin panel for AuraJewels team to onboard jewellery organizations, manage stores, control feature access per store, and monitor the platform.
> **Frontend:** React.js (separate repo)
> **Backend:** New endpoints in existing Spring Boot backend

---

## Table of Contents

1. [Concept Overview](#1-concept-overview)
2. [User Hierarchy (Updated)](#2-user-hierarchy-updated)
3. [Feature Module System](#3-feature-module-system)
4. [Database Changes](#4-database-changes)
5. [Backend API Endpoints](#5-backend-api-endpoints)
6. [Super Admin Panel UI (React.js)](#6-super-admin-panel-ui-reactjs)
7. [Onboarding Flow (Step-by-Step)](#7-onboarding-flow-step-by-step)
8. [How Feature Gating Works](#8-how-feature-gating-works)
9. [React.js Project Structure](#9-reactjs-project-structure)
10. [Implementation Plan](#10-implementation-plan)

---

## 1. Concept Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    SUPER ADMIN PANEL (React.js)                  │
│                    (Only AuraJewels team uses this)              │
│                                                                  │
│   Onboard Org → Create Stores → Assign Features → Create Owner  │
└─────────────────────┬───────────────────────────────────────────┘
                      │ /api/admin/**
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT BACKEND                           │
│                    (Existing jewel-erp backend)                  │
└─────────┬──────────────────┬────────────────────┬───────────────┘
          │                  │                    │
          ▼                  ▼                    ▼
┌──────────────┐   ┌──────────────┐    ┌──────────────────┐
│ Desktop App  │   │ Admin Mobile │    │  Customer App    │
│ (Electron)   │   │ (Flutter)    │    │  (Flutter)       │
│              │   │              │    │                  │
│ Owner sees   │   │ Owner sees   │    │ Customers browse │
│ ONLY modules │   │ ONLY modules │    │ catalog, enquire │
│ assigned to  │   │ assigned to  │    │                  │
│ their store  │   │ their store  │    │                  │
└──────────────┘   └──────────────┘    └──────────────────┘
```

**Who uses what:**

| Panel | Users | Purpose |
|-------|-------|---------|
| **Super Admin Panel** (React.js) | AuraJewels team (you) | Onboard orgs, manage stores, assign features, monitor platform |
| **Desktop App** (Electron.js) | Jewellery store Owner/Admin/Staff | Day-to-day store operations |
| **Admin Mobile App** (Flutter) | Jewellery store Owner/Admin/Staff | Same as desktop, on mobile |
| **Customer App** (Flutter) | End customers | Browse, wishlist, enquire |

---

## 2. User Hierarchy (Updated)

```
SUPER_ADMIN (AuraJewels team — you)
  │
  ├── Organization 1 (e.g., "PNG Jewellers")
  │     ├── Store A (Satara Branch)
  │     │     ├── OWNER (Prakash Gupta) — sees modules: Billing, Inventory, Schemes
  │     │     ├── ADMIN (Rohit Sharma) — same modules as store
  │     │     └── STAFF (Priya Patil) — permissions within allowed modules
  │     │
  │     └── Store B (Pune Branch)
  │           ├── OWNER (same Prakash Gupta) — sees modules: Billing, Inventory
  │           ├── ADMIN (Neha Joshi)
  │           └── STAFF (...)
  │
  ├── Organization 2 (e.g., "Tanishq")
  │     └── Store C (Mumbai Branch)
  │           └── ...
  │
  └── Organization N (...)
```

**New role: `SUPER_ADMIN`**
- Only AuraJewels team members
- Can see ALL organizations, ALL stores
- Can onboard, modify, suspend organizations
- Cannot be created from Desktop/Mobile apps — only via backend/DB directly

---

## 3. Feature Module System

### Available Modules

Each store can have a subset of these modules enabled:

| Module ID | Module Name | What It Controls | Default |
|-----------|-------------|------------------|---------|
| `DASHBOARD` | Dashboard | `/api/dashboard/**` | ✅ Always ON |
| `INVENTORY` | Inventory | `/api/jewelry-items/**`, `/api/categories/**`, `/api/metal-types/**` | ✅ |
| `BILLING` | Billing | `/api/invoices/**`, `/api/estimates/**`, `/api/credit-notes/**` | ✅ |
| `CUSTOMERS` | Customers | `/api/customers/**`, `/api/enquiries/**` | ✅ |
| `ACCOUNTS` | Accounts | `/api/ledger/**`, `/api/expenses/**`, `/api/cash-register/**`, `/api/suppliers/**`, `/api/old-gold/**` | ❌ |
| `RATES` | Daily Rates | `/api/rates/**` | ❌ |
| `SCHEMES` | Saving Schemes | `/api/schemes/**` | ❌ |
| `REPORTS` | Reports & Logs | `/api/activity-logs/**`, advanced reports | ❌ |
| `NOTIFICATIONS` | Bulk SMS/Push | `/api/notifications/**` | ❌ |
| `CUSTOMER_APP` | Customer App | Customer app shows this store's catalog | ❌ |
| `SETTINGS` | Settings | `/api/settings/**` | ✅ Always ON |

### Pricing Plan → Module Mapping

| Plan | Modules Included |
|------|-----------------|
| **Starter** (₹799/mo) | DASHBOARD, INVENTORY, BILLING, CUSTOMERS, RATES, SETTINGS |
| **Professional** (₹1,499/mo) | All Starter + ACCOUNTS, SCHEMES, REPORTS, CUSTOMER_APP |
| **Enterprise** (₹2,999/mo) | All Professional + NOTIFICATIONS + unlimited stores |
| **Custom** | Super Admin selects specific modules per store |

### How It Works

1. Super Admin onboards organization + stores
2. Super Admin selects which modules each store gets (or picks a plan template)
3. Owner logs in → JWT includes `enabledModules` for the current store
4. Desktop/Mobile app reads `enabledModules` and shows/hides sidebar items
5. Backend also enforces — if a module is disabled, API returns `403 Module not enabled`

---

## 4. Database Changes

### New Tables

```sql
-- ============================================================
-- V6__super_admin_feature_modules.sql
-- ============================================================

-- Super admin users (AuraJewels team only)
-- These are separate from the regular `users` table
CREATE TABLE platform_admins (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    phone       VARCHAR(15),
    password_hash VARCHAR(255) NOT NULL,
    role        ENUM('SUPER_ADMIN', 'SUPPORT') NOT NULL DEFAULT 'SUPER_ADMIN',
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Feature modules master list
CREATE TABLE feature_modules (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50) NOT NULL UNIQUE,   -- 'BILLING', 'INVENTORY', etc.
    name        VARCHAR(100) NOT NULL,          -- 'Billing & Invoicing'
    description VARCHAR(500),
    is_core     BOOLEAN NOT NULL DEFAULT FALSE, -- core modules can't be disabled
    sort_order  INT NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Which modules are enabled for which store
CREATE TABLE store_feature_modules (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id    BIGINT NOT NULL,
    module_id   BIGINT NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    enabled_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    enabled_by  BIGINT,                         -- platform_admin who enabled it
    UNIQUE KEY uk_store_module (store_id, module_id),
    FOREIGN KEY (store_id) REFERENCES stores(id),
    FOREIGN KEY (module_id) REFERENCES feature_modules(id)
);

-- Subscription/plan tracking per organization
CREATE TABLE org_subscriptions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    org_id          BIGINT NOT NULL,
    plan            ENUM('STARTER', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM') NOT NULL DEFAULT 'STARTER',
    status          ENUM('ACTIVE', 'TRIAL', 'SUSPENDED', 'CANCELLED') NOT NULL DEFAULT 'TRIAL',
    trial_ends_at   TIMESTAMP,
    current_period_start TIMESTAMP,
    current_period_end   TIMESTAMP,
    max_stores      INT NOT NULL DEFAULT 1,
    max_users       INT NOT NULL DEFAULT 2,
    notes           TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (org_id) REFERENCES organizations(id)
);

-- Audit log for super admin actions
CREATE TABLE platform_audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id    BIGINT NOT NULL,
    admin_name  VARCHAR(100),
    action      VARCHAR(100) NOT NULL,      -- 'ONBOARD_ORG', 'ENABLE_MODULE', etc.
    entity_type VARCHAR(50),                -- 'ORGANIZATION', 'STORE', 'MODULE'
    entity_id   BIGINT,
    detail      TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES platform_admins(id)
);

-- Seed the feature modules
INSERT INTO feature_modules (code, name, description, is_core, sort_order) VALUES
('DASHBOARD',     'Dashboard',           'Sales summary and metrics',                    TRUE,  1),
('INVENTORY',     'Inventory Management','Items, categories, metal types',               FALSE, 2),
('BILLING',       'Billing & Invoicing', 'Invoices, estimates, credit notes',            FALSE, 3),
('CUSTOMERS',     'Customer Management', 'Customer database and enquiries',              FALSE, 4),
('ACCOUNTS',      'Accounts',            'Ledger, expenses, cash register, suppliers',   FALSE, 5),
('RATES',         'Daily Rates',         'Gold, silver, platinum rate management',       FALSE, 6),
('SCHEMES',       'Saving Schemes',      'Customer saving scheme management',            FALSE, 7),
('REPORTS',       'Reports & Analytics', 'Activity logs, advanced reports',              FALSE, 8),
('NOTIFICATIONS', 'Notifications',       'Bulk SMS, push notifications',                 FALSE, 9),
('CUSTOMER_APP',  'Customer App',        'Customer mobile app catalog for this store',   FALSE, 10),
('SETTINGS',      'Settings',            'Store profile and configuration',              TRUE,  11);
```

### Changes to Existing Tables

```sql
-- Add subscription reference to organizations
ALTER TABLE organizations ADD COLUMN onboarded_by BIGINT NULL;
ALTER TABLE organizations ADD COLUMN onboarded_at TIMESTAMP NULL;
ALTER TABLE organizations ADD COLUMN status ENUM('ACTIVE', 'TRIAL', 'SUSPENDED', 'CANCELLED') DEFAULT 'ACTIVE';

-- Add notes/metadata to stores
ALTER TABLE stores ADD COLUMN status ENUM('ACTIVE', 'SUSPENDED') DEFAULT 'ACTIVE';
```

---

## 5. Backend API Endpoints

### New: Super Admin APIs — `/api/admin/**`

**Auth:** Requires `SUPER_ADMIN` JWT (separate from regular user JWT).

#### 5.1 Super Admin Auth

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | `POST` | `/api/admin/auth/login` | Super admin login (email + password) |
| 2 | `POST` | `/api/admin/auth/refresh` | Refresh token |

**Login Request:**
```json
{
  "email": "raviraj@aurajewels.com",
  "password": "superSecurePass"
}
```

**Login Response:**
```json
{
  "token": "eyJ...",
  "refreshToken": "eyJ...",
  "role": "SUPER_ADMIN",
  "name": "Raviraj Bhosale"
}
```

#### 5.2 Organization Management

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 3 | `GET` | `/api/admin/organizations` | List all organizations |
| 4 | `GET` | `/api/admin/organizations/{id}` | Get org with stores, owner, subscription |
| 5 | `POST` | `/api/admin/organizations` | Onboard new organization (creates org + stores + owner) |
| 6 | `PUT` | `/api/admin/organizations/{id}` | Update org details |
| 7 | `PATCH` | `/api/admin/organizations/{id}/status` | Suspend / activate org |

**POST — Onboard Organization (the big one):**
```json
{
  "organization": {
    "name": "PNG Jewellers",
    "gstin": "27AABCX1234Z1",
    "pan": "AABCX1234Z",
    "address": "MG Road, Satara",
    "phone": "02162-234567",
    "email": "info@pngjewellers.com"
  },
  "owner": {
    "name": "Prakash Gupta",
    "mobile": "9876543210",
    "email": "prakash@pngjewellers.com",
    "password": "TempPass@123"
  },
  "stores": [
    {
      "name": "PNG Satara",
      "address": "MG Road, Satara, Maharashtra",
      "city": "Satara",
      "phone": "02162-234567",
      "gstin": "27AABCX1234Z1"
    },
    {
      "name": "PNG Pune",
      "address": "FC Road, Pune, Maharashtra",
      "city": "Pune",
      "phone": "020-25678901",
      "gstin": "27AABCX1234Z2"
    }
  ],
  "subscription": {
    "plan": "PROFESSIONAL",
    "trialDays": 90,
    "maxStores": 2,
    "maxUsers": 10
  }
}
```

**Response:**
```json
{
  "organizationId": 5,
  "organizationName": "PNG Jewellers",
  "ownerId": 42,
  "ownerMobile": "9876543210",
  "ownerTempPassword": "TempPass@123",
  "forcePasswordChange": true,
  "stores": [
    {"id": 8, "name": "PNG Satara"},
    {"id": 9, "name": "PNG Pune"}
  ],
  "subscription": {
    "plan": "PROFESSIONAL",
    "status": "TRIAL",
    "trialEndsAt": "2026-06-17T00:00:00Z"
  }
}
```

> After this, you share `mobile: 9876543210` and `password: TempPass@123` with the owner. On first login, they'll be forced to change password.

#### 5.3 Store Management

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 8 | `GET` | `/api/admin/organizations/{orgId}/stores` | List stores in org |
| 9 | `POST` | `/api/admin/organizations/{orgId}/stores` | Add new store to existing org |
| 10 | `PUT` | `/api/admin/stores/{storeId}` | Update store details |
| 11 | `PATCH` | `/api/admin/stores/{storeId}/status` | Suspend / activate store |

#### 5.4 Feature Module Management (THE KEY PART)

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 12 | `GET` | `/api/admin/modules` | List all available modules |
| 13 | `GET` | `/api/admin/stores/{storeId}/modules` | Get modules enabled for a store |
| 14 | `PUT` | `/api/admin/stores/{storeId}/modules` | Set enabled modules for a store |
| 15 | `POST` | `/api/admin/stores/{storeId}/modules/{moduleCode}/enable` | Enable single module |
| 16 | `POST` | `/api/admin/stores/{storeId}/modules/{moduleCode}/disable` | Disable single module |

**PUT — Set Modules for Store (bulk):**
```json
{
  "modules": ["DASHBOARD", "INVENTORY", "BILLING", "CUSTOMERS", "RATES", "SETTINGS"]
}
```

**Response:**
```json
{
  "storeId": 8,
  "storeName": "PNG Satara",
  "enabledModules": [
    {"code": "DASHBOARD", "name": "Dashboard", "isCore": true},
    {"code": "INVENTORY", "name": "Inventory Management", "isCore": false},
    {"code": "BILLING", "name": "Billing & Invoicing", "isCore": false},
    {"code": "CUSTOMERS", "name": "Customer Management", "isCore": false},
    {"code": "RATES", "name": "Daily Rates", "isCore": false},
    {"code": "SETTINGS", "name": "Settings", "isCore": true}
  ],
  "disabledModules": [
    {"code": "ACCOUNTS", "name": "Accounts"},
    {"code": "SCHEMES", "name": "Saving Schemes"},
    {"code": "REPORTS", "name": "Reports & Analytics"},
    {"code": "NOTIFICATIONS", "name": "Notifications"},
    {"code": "CUSTOMER_APP", "name": "Customer App"}
  ]
}
```

#### 5.5 Subscription Management

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 17 | `GET` | `/api/admin/organizations/{orgId}/subscription` | Get current subscription |
| 18 | `PUT` | `/api/admin/organizations/{orgId}/subscription` | Update plan/limits |

#### 5.6 Platform Dashboard

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 19 | `GET` | `/api/admin/dashboard` | Platform-wide stats |

**Response:**
```json
{
  "totalOrganizations": 45,
  "activeOrganizations": 42,
  "trialOrganizations": 8,
  "totalStores": 78,
  "totalUsers": 312,
  "totalCustomers": 15600,
  "mrr": 89925,
  "planBreakdown": {
    "STARTER": 20,
    "PROFESSIONAL": 18,
    "ENTERPRISE": 4,
    "CUSTOM": 3
  }
}
```

#### 5.7 Platform Audit Logs

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 20 | `GET` | `/api/admin/audit-logs` | List all super admin actions |

---

### Changes to Existing APIs

#### Login Response (Updated)

When a regular user (Owner/Admin/Staff) logs in via `POST /api/auth/login`, the response now includes enabled modules:

```json
{
  "token": "eyJ...",
  "refreshToken": "eyJ...",
  "role": "OWNER",
  "userName": "Prakash Gupta",
  "forcePasswordChange": false,
  "stores": [
    {
      "id": 8,
      "name": "PNG Satara",
      "city": "Satara",
      "enabledModules": ["DASHBOARD", "INVENTORY", "BILLING", "CUSTOMERS", "RATES", "SETTINGS"]
    },
    {
      "id": 9,
      "name": "PNG Pune",
      "city": "Pune",
      "enabledModules": ["DASHBOARD", "INVENTORY", "BILLING", "CUSTOMERS", "ACCOUNTS", "RATES", "SCHEMES", "SETTINGS"]
    }
  ]
}
```

> **Desktop/Mobile app reads `enabledModules` per store and shows/hides sidebar items accordingly.**

#### Backend Module Enforcement

Every admin API endpoint checks if the module is enabled:

```
GET /api/schemes         → check if SCHEMES module is enabled for current store
                         → if not → 403 {"error": "Module not enabled", "module": "SCHEMES"}

GET /api/invoices        → check if BILLING module is enabled
                         → if not → 403 {"error": "Module not enabled", "module": "BILLING"}
```

---

## 6. Super Admin Panel UI (React.js)

### Pages & Layout

```
┌──────────────────────────────────────────────────────────────────┐
│  🔷 JewelERP Admin                          👤 Raviraj  [Logout]│
├────────────┬─────────────────────────────────────────────────────┤
│            │                                                     │
│  📊 Dashboard│     DASHBOARD                                    │
│            │     ┌─────────┐ ┌─────────┐ ┌─────────┐           │
│  🏢 Orgs   │     │ 45 Orgs │ │ 78 Store│ │ ₹89,925 │           │
│            │     │ Active  │ │ Total   │ │  MRR    │           │
│  🏪 Stores │     └─────────┘ └─────────┘ └─────────┘           │
│            │                                                     │
│  📦 Modules│     Plan Breakdown:                                │
│            │     ██████████░░ Starter: 20                       │
│  💳 Plans  │     ████████████ Professional: 18                  │
│            │     ████░░░░░░░░ Enterprise: 4                     │
│  📋 Audit  │                                                     │
│            │     Recent Onboardings:                             │
│  ⚙️ Settings│     • PNG Jewellers — 2 stores — Trial            │
│            │     • Kalyan — 5 stores — Professional              │
│            │                                                     │
└────────────┴─────────────────────────────────────────────────────┘
```

### Page List

| # | Page | Route | Description |
|---|------|-------|-------------|
| 1 | **Login** | `/login` | Super admin email + password login |
| 2 | **Dashboard** | `/dashboard` | Platform stats, MRR, plan breakdown |
| 3 | **Organizations** | `/organizations` | List all orgs with search, filter |
| 4 | **Organization Detail** | `/organizations/:id` | Org details, stores list, subscription |
| 5 | **Onboard Organization** | `/organizations/new` | Multi-step form to onboard |
| 6 | **Store Detail** | `/stores/:id` | Store details + module management |
| 7 | **Module Management** | `/stores/:id/modules` | Toggle modules on/off (checkboxes) |
| 8 | **All Stores** | `/stores` | Global store list across all orgs |
| 9 | **Subscription Management** | `/organizations/:id/subscription` | Change plan, extend trial |
| 10 | **Audit Logs** | `/audit-logs` | All super admin actions |
| 11 | **Settings** | `/settings` | Platform settings, add support users |

### Key UI Components

#### Onboarding Form (Multi-Step Wizard)

```
Step 1: Organization Details          Step 2: Owner Details
┌────────────────────────────┐       ┌────────────────────────────┐
│ Organization Name: [______]│       │ Owner Name:    [__________]│
│ GSTIN:             [______]│       │ Mobile:        [__________]│
│ PAN:               [______]│       │ Email:         [__________]│
│ Address:           [______]│       │ Temp Password: [__________]│
│ Phone:             [______]│       │                            │
│ Email:             [______]│       │ [Auto-generate password]   │
│                            │       │                            │
│              [Next →]      │       │    [← Back]  [Next →]     │
└────────────────────────────┘       └────────────────────────────┘

Step 3: Add Stores                    Step 4: Select Plan & Modules
┌────────────────────────────┐       ┌────────────────────────────┐
│ Store 1:                   │       │ Plan: (●) Professional     │
│   Name:  [PNG Satara     ] │       │       ( ) Starter          │
│   City:  [Satara         ] │       │       ( ) Enterprise       │
│   Phone: [02162-234567   ] │       │       ( ) Custom           │
│   GSTIN: [27AABCX1234Z1 ] │       │                            │
│                            │       │ Trial Period: [90] days    │
│ [+ Add Another Store]      │       │ Max Stores:   [2]          │
│                            │       │ Max Users:    [10]         │
│ Store 2:                   │       │                            │
│   Name:  [PNG Pune       ] │       │ ── Per-Store Modules ──    │
│   City:  [Pune           ] │       │                            │
│   ...                      │       │ PNG Satara:                │
│                            │       │ ☑ Dashboard  ☑ Inventory   │
│    [← Back]  [Next →]     │       │ ☑ Billing    ☑ Customers   │
└────────────────────────────┘       │ ☐ Accounts   ☑ Rates      │
                                     │ ☐ Schemes    ☐ Reports    │
                                     │ ☐ Notifications            │
                                     │ ☐ Customer App             │
                                     │ ☑ Settings                 │
                                     │                            │
                                     │ PNG Pune:                  │
                                     │ ☑ Dashboard  ☑ Inventory   │
                                     │ ☑ Billing    ☑ Customers   │
                                     │ ☑ Accounts   ☑ Rates      │
                                     │ ☑ Schemes    ☐ Reports    │
                                     │ ☐ Notifications            │
                                     │ ☐ Customer App             │
                                     │ ☑ Settings                 │
                                     │                            │
                                     │ [← Back]  [🚀 Onboard]   │
                                     └────────────────────────────┘

Step 5: Confirmation
┌────────────────────────────────────────────────────────────┐
│  ✅ Organization onboarded successfully!                    │
│                                                             │
│  Organization: PNG Jewellers                                │
│  Owner: Prakash Gupta                                       │
│  Mobile: 9876543210                                         │
│  Temp Password: TmpPNG@2026                                 │
│                                                             │
│  Stores:                                                    │
│    • PNG Satara (6 modules)                                 │
│    • PNG Pune (8 modules)                                   │
│                                                             │
│  Plan: Professional (90-day trial)                          │
│                                                             │
│  📋 Share these credentials with the owner:                 │
│  ┌──────────────────────────────────────┐                   │
│  │ Login: 9876543210                    │  [📋 Copy]       │
│  │ Password: TmpPNG@2026               │                   │
│  │ (Must change on first login)         │                   │
│  └──────────────────────────────────────┘                   │
│                                                             │
│  [📤 Send WhatsApp] [📧 Send Email] [Go to Dashboard]     │
└────────────────────────────────────────────────────────────┘
```

#### Module Management (Per Store)

```
┌────────────────────────────────────────────────────────────┐
│  🏪 PNG Satara — Feature Modules                           │
│  Plan: Professional | Status: Active                       │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  Core Modules (always enabled):                            │
│  ☑ Dashboard          ☑ Settings                          │
│                                                            │
│  Optional Modules:                                         │
│  ┌────────────────────────┬──────────┬──────────────────┐ │
│  │ Module                 │ Status   │ Action            │ │
│  ├────────────────────────┼──────────┼──────────────────┤ │
│  │ 📦 Inventory           │ 🟢 ON   │ [Disable]        │ │
│  │ 🧾 Billing             │ 🟢 ON   │ [Disable]        │ │
│  │ 👥 Customers           │ 🟢 ON   │ [Disable]        │ │
│  │ 📊 Accounts            │ 🔴 OFF  │ [Enable]         │ │
│  │ 💰 Daily Rates         │ 🟢 ON   │ [Disable]        │ │
│  │ 💎 Saving Schemes      │ 🔴 OFF  │ [Enable]         │ │
│  │ 📈 Reports             │ 🔴 OFF  │ [Enable]         │ │
│  │ 📨 Notifications       │ 🔴 OFF  │ [Enable]         │ │
│  │ 📱 Customer App        │ 🔴 OFF  │ [Enable]         │ │
│  └────────────────────────┴──────────┴──────────────────┘ │
│                                                            │
│  Quick Apply Plan:                                         │
│  [Starter] [Professional] [Enterprise] [Custom]            │
│                                                            │
│  [💾 Save Changes]                                         │
└────────────────────────────────────────────────────────────┘
```

---

## 7. Onboarding Flow (Step-by-Step)

### What Happens When You Click "Onboard"

```
Super Admin clicks "🚀 Onboard"
        │
        ▼
POST /api/admin/organizations
        │
        ▼ Backend does (in one transaction):
        │
        ├── 1. CREATE organization record
        │
        ├── 2. CREATE store records (1 or more)
        │
        ├── 3. CREATE owner user (with temp password, forcePasswordChange=true)
        │
        ├── 4. CREATE user_store_access (owner → all stores)
        │
        ├── 5. CREATE org_subscription (plan, trial, limits)
        │
        ├── 6. For each store:
        │       CREATE store_feature_modules (enabled modules)
        │
        ├── 7. SEED default data per store:
        │       • Default categories (Rings, Necklaces, etc.)
        │       • Default metal types (Gold 24K, Gold 22K, Silver, etc.)
        │       • Default permissions
        │       • Default store settings
        │
        ├── 8. CREATE platform_audit_log ("ONBOARD_ORG")
        │
        └── 9. Return success with credentials
        
        ▼
Super Admin shares credentials with owner via WhatsApp/Email/Call
        │
        ▼
Owner logs in → forced to change password → sees only enabled modules
```

---

## 8. How Feature Gating Works

### On Login (JWT + API Response)

```
Owner logs in → POST /api/auth/login
                     │
                     ▼
              Backend checks store_feature_modules
              for each store the owner has access to
                     │
                     ▼
              Returns enabledModules[] per store in login response
                     │
                     ▼
              Desktop/Mobile app reads enabledModules
              → Shows/hides sidebar items
```

### On Every API Call (Backend Enforcement)

```
Staff calls GET /api/schemes
                     │
                     ▼
              Backend checks:
              1. JWT valid? ✅
              2. User has permission? ✅ (MANAGE_SCHEME)
              3. Module SCHEMES enabled for this store? ❌
                     │
                     ▼
              Returns 403:
              {"error": "Module not enabled for this store", "module": "SCHEMES"}
```

### Implementation — Module Check Annotation

```java
// New annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresModule {
    String value(); // e.g., "BILLING", "SCHEMES"
}

// Usage in controllers
@GetMapping
@RequiresPermission("VIEW_BILLING")
@RequiresModule("BILLING")           // ← NEW: checks if module is enabled
public ResponseEntity<List<Invoice>> listInvoices() { ... }

@GetMapping
@RequiresPermission("MANAGE_SCHEME")
@RequiresModule("SCHEMES")           // ← NEW
public ResponseEntity<List<Scheme>> listSchemes() { ... }
```

### Desktop/Mobile App — Sidebar Logic

```typescript
// In Electron.js / Flutter
const sidebarItems = [
  { label: "Dashboard",   icon: "📊", module: "DASHBOARD",     route: "/dashboard" },
  { label: "Inventory",   icon: "📦", module: "INVENTORY",     route: "/inventory" },
  { label: "Billing",     icon: "🧾", module: "BILLING",       route: "/billing" },
  { label: "Customers",   icon: "👥", module: "CUSTOMERS",     route: "/customers" },
  { label: "Accounts",    icon: "📊", module: "ACCOUNTS",      route: "/accounts" },
  { label: "Rates",       icon: "💰", module: "RATES",         route: "/rates" },
  { label: "Schemes",     icon: "💎", module: "SCHEMES",       route: "/schemes" },
  { label: "Reports",     icon: "📈", module: "REPORTS",       route: "/reports" },
  { label: "Enquiries",   icon: "📨", module: "CUSTOMERS",     route: "/enquiries" },
  { label: "Settings",    icon: "⚙️", module: "SETTINGS",      route: "/settings" },
];

// Filter based on enabledModules from login response
const visibleItems = sidebarItems.filter(item =>
  store.enabledModules.includes(item.module)
);
```

---

## 9. React.js Project Structure

```
jewel-erp-admin/                      # Separate repo
├── public/
│   └── index.html
├── src/
│   ├── api/
│   │   ├── adminApi.ts               # Axios instance with auth interceptor
│   │   ├── authApi.ts                # Login, refresh
│   │   ├── organizationApi.ts        # Org CRUD
│   │   ├── storeApi.ts               # Store CRUD
│   │   ├── moduleApi.ts              # Module management
│   │   ├── subscriptionApi.ts        # Plan management
│   │   └── dashboardApi.ts           # Platform stats
│   │
│   ├── components/
│   │   ├── layout/
│   │   │   ├── Sidebar.tsx
│   │   │   ├── Header.tsx
│   │   │   └── MainLayout.tsx
│   │   ├── common/
│   │   │   ├── Table.tsx
│   │   │   ├── Modal.tsx
│   │   │   ├── Badge.tsx
│   │   │   ├── SearchInput.tsx
│   │   │   └── Spinner.tsx
│   │   ├── organizations/
│   │   │   ├── OrgList.tsx
│   │   │   ├── OrgDetail.tsx
│   │   │   ├── OrgOnboardWizard.tsx  # Multi-step form
│   │   │   └── OrgStatusBadge.tsx
│   │   ├── stores/
│   │   │   ├── StoreList.tsx
│   │   │   ├── StoreDetail.tsx
│   │   │   └── ModuleManager.tsx     # Toggle modules on/off
│   │   ├── subscriptions/
│   │   │   ├── PlanSelector.tsx
│   │   │   └── SubscriptionCard.tsx
│   │   ├── dashboard/
│   │   │   ├── StatsCards.tsx
│   │   │   ├── PlanChart.tsx
│   │   │   └── RecentOnboardings.tsx
│   │   └── audit/
│   │       └── AuditLogTable.tsx
│   │
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── DashboardPage.tsx
│   │   ├── OrganizationsPage.tsx
│   │   ├── OrganizationDetailPage.tsx
│   │   ├── OnboardPage.tsx
│   │   ├── StoresPage.tsx
│   │   ├── StoreDetailPage.tsx
│   │   ├── AuditLogPage.tsx
│   │   └── SettingsPage.tsx
│   │
│   ├── hooks/
│   │   ├── useAuth.ts
│   │   ├── useOrganizations.ts
│   │   ├── useStores.ts
│   │   └── useModules.ts
│   │
│   ├── store/                        # Zustand or Redux
│   │   ├── authStore.ts
│   │   └── platformStore.ts
│   │
│   ├── types/
│   │   ├── organization.ts
│   │   ├── store.ts
│   │   ├── module.ts
│   │   ├── subscription.ts
│   │   └── auth.ts
│   │
│   ├── utils/
│   │   ├── constants.ts
│   │   └── formatters.ts
│   │
│   ├── App.tsx
│   ├── routes.tsx
│   └── main.tsx
│
├── package.json
├── tsconfig.json
├── vite.config.ts                    # Vite for fast dev
├── tailwind.config.js                # Tailwind CSS
└── README.md
```

### Recommended React Tech Stack

| Library | Purpose |
|---------|---------|
| **React 18** | UI framework |
| **TypeScript** | Type safety |
| **Vite** | Build tool (fast dev server) |
| **Tailwind CSS** | Styling |
| **React Router v6** | Routing |
| **Zustand** (or Redux Toolkit) | State management |
| **Axios** | HTTP client |
| **React Hook Form** | Form handling (onboard wizard) |
| **Zod** | Form validation |
| **Recharts** | Dashboard charts |
| **React Hot Toast** | Notifications |
| **Lucide React** | Icons |

---

## 10. Implementation Plan

### Phase 1: Backend — Super Admin APIs (1-2 weeks)

| # | Task | Effort |
|---|------|--------|
| 1 | Create `V6__super_admin_feature_modules.sql` migration | 2 hours |
| 2 | Create `PlatformAdmin` entity + repository | 1 hour |
| 3 | Create `FeatureModule` entity + repository | 1 hour |
| 4 | Create `StoreFeatureModule` entity + repository | 1 hour |
| 5 | Create `OrgSubscription` entity + repository | 1 hour |
| 6 | Create `PlatformAuditLog` entity + repository | 1 hour |
| 7 | Create `SuperAdminAuthController` (login, refresh) | 3 hours |
| 8 | Create `SuperAdminJwtFilter` (separate from regular JWT) | 3 hours |
| 9 | Create `OrganizationAdminController` (CRUD + onboard) | 4 hours |
| 10 | Create `StoreAdminController` (CRUD + module management) | 3 hours |
| 11 | Create `SubscriptionAdminController` | 2 hours |
| 12 | Create `PlatformDashboardController` | 2 hours |
| 13 | Create `@RequiresModule` annotation + aspect | 3 hours |
| 14 | Update login response to include `enabledModules` per store | 2 hours |
| 15 | Add module checks to all existing controllers | 4 hours |
| 16 | Update SecurityConfig for `/api/admin/**` | 1 hour |

### Phase 2: React.js — Super Admin Panel (2-3 weeks)

| # | Task | Effort |
|---|------|--------|
| 1 | Setup React + Vite + TypeScript + Tailwind | 2 hours |
| 2 | Login page + auth flow | 4 hours |
| 3 | Layout (sidebar, header, main) | 4 hours |
| 4 | Dashboard page with stats | 4 hours |
| 5 | Organizations list page | 4 hours |
| 6 | Organization detail page | 4 hours |
| 7 | **Onboard wizard (multi-step form)** | 8 hours |
| 8 | Store detail page | 3 hours |
| 9 | **Module manager (toggle on/off)** | 4 hours |
| 10 | Subscription management | 3 hours |
| 11 | Audit logs page | 2 hours |
| 12 | Testing & polish | 4 hours |

### Phase 3: Integration — Desktop/Mobile App Changes (1 week)

| # | Task | Effort |
|---|------|--------|
| 1 | Read `enabledModules` from login response | 1 hour |
| 2 | Filter sidebar items based on enabled modules | 2 hours |
| 3 | Handle `403 Module not enabled` error gracefully | 2 hours |
| 4 | Test with different module configurations | 4 hours |

### Total Effort

| Phase | Effort |
|-------|--------|
| Backend APIs | ~30 hours (1-2 weeks) |
| React Admin Panel | ~40 hours (2-3 weeks) |
| Desktop/Mobile integration | ~10 hours (1 week) |
| **Total** | **~80 hours (4-6 weeks)** |

---

## Summary

| What | Details |
|------|---------|
| **New role** | `SUPER_ADMIN` — AuraJewels team only |
| **New panel** | React.js super admin panel (separate repo) |
| **New DB tables** | `platform_admins`, `feature_modules`, `store_feature_modules`, `org_subscriptions`, `platform_audit_logs` |
| **New APIs** | ~20 endpoints at `/api/admin/**` |
| **Key feature** | Per-store module toggling — select which features each store gets |
| **How it works** | Login response includes `enabledModules[]` → Desktop/Mobile shows/hides sidebar → Backend enforces with `@RequiresModule` |
| **Deployment** | React app can be hosted on S3 + CloudFront (static site, ₹50/month) or Vercel (free) |
