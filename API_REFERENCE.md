# JewelERP — Complete API & Database Reference

> **For Frontend Developers** — Everything you need to integrate with the JewelERP backend.
>
> **Last Updated:** 2026-03-14
> **Total Endpoints:** 103
> **Total DB Tables:** 33

---

## Table of Contents

- [1. Getting Started](#1-getting-started)
- [2. Authentication & Authorization](#2-authentication--authorization)
- [3. API Endpoints](#3-api-endpoints)
  - [3.1 Auth](#31-auth--apiauth)
  - [3.2 Staff](#32-staff--apistaff)
  - [3.3 Categories](#33-categories--apicategories)
  - [3.4 Metal Types](#34-metal-types--apimetal-types)
  - [3.5 Jewelry Items (Inventory)](#35-jewelry-items--apijewelry-items)
  - [3.6 Customers](#36-customers--apicustomers)
  - [3.7 Invoices](#37-invoices--apiinvoices)
  - [3.8 Estimates](#38-estimates--apiestimates)
  - [3.9 Credit Notes](#39-credit-notes--apicredit-notes)
  - [3.10 Ledger](#310-ledger--apiledger)
  - [3.11 Expenses](#311-expenses--apiexpenses)
  - [3.12 Cash Register](#312-cash-register--apicash-register)
  - [3.13 Suppliers](#313-suppliers--apisuppliers)
  - [3.14 Daily Rates](#314-daily-rates--apirates)
  - [3.15 Old Gold](#315-old-gold--apiold-gold)
  - [3.16 Saving Schemes](#316-saving-schemes--apischemes)
  - [3.17 Dashboard](#317-dashboard--apidashboard)
  - [3.18 Settings](#318-settings--apisettings)
  - [3.19 Activity Logs](#319-activity-logs--apiactivity-logs)
  - [3.20 DB Admin](#320-db-admin--apidb)
  - [3.21 Health / Hello](#321-health--hello)
- [4. Database Schema — All Tables & Columns](#4-database-schema--all-tables--columns)
- [5. Entity Relationship Diagram (ERD)](#5-entity-relationship-diagram-erd)
- [6. Permissions Master List](#6-permissions-master-list)
- [7. Seed Data / Test Users](#7-seed-data--test-users)
- [8. Error Handling](#8-error-handling)
- [9. Endpoint Count Summary](#9-endpoint-count-summary)

---

## 1. Getting Started

### Base URL

```
Production:  http://<ALB_DNS>/api
Local Dev:   http://localhost:8080/api
```

### Headers (for all authenticated requests)

```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
X-Store-Id: <STORE_ID>          # Optional — defaults to JWT's storeId
```

### Date Formats

| Type | Format | Example |
|------|--------|---------|
| Date | `YYYY-MM-DD` | `2026-03-14` |
| Timestamp | ISO-8601 UTC | `2026-03-14T10:30:00.000Z` |
| Decimals | Up to 2 decimal places | `6900.00` |
| Weight | Up to 3 decimal places | `5.200` |

---

## 2. Authentication & Authorization

### JWT Token — Decoded Payload

```json
{
  "sub": "1",
  "orgId": 1,
  "storeId": 1,
  "role": "OWNER",
  "permissions": [],
  "name": "Admin",
  "iat": 1773032419,
  "exp": 1773118819
}
```

| Field | Type | Description |
|-------|------|-------------|
| `sub` | String | User ID |
| `orgId` | Long | Organization ID |
| `storeId` | Long | Currently selected store ID |
| `role` | String | `OWNER`, `ADMIN`, or `STAFF` |
| `permissions` | String[] | Permission codes (only for STAFF) |
| `name` | String | User display name |
| `iat` | Long | Issued at (epoch seconds) |
| `exp` | Long | Expiry (24h for access, 7d for refresh) |

### Role Hierarchy

```
OWNER   → Full access to ALL stores in the organization. Bypasses all permission checks.
ADMIN   → Full access to assigned stores only. Bypasses permission checks.
STAFF   → Access only to endpoints matching assigned permissions for assigned stores.
```

### Auth Flow (Frontend)

```
1. POST /api/auth/login  →  Get { token, refreshToken, stores[], forcePasswordChange }
2. If forcePasswordChange === true  →  Redirect to change-password screen
3. If stores.length > 1  →  Show store picker (re-login with selected storeId)
4. Set Authorization header: Bearer <token>
5. On 401 response  →  POST /api/auth/refresh-token with refreshToken
6. On refresh failure  →  Redirect to login
```

---

## 3. API Endpoints

### Public Endpoints (No Auth Required)

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | `GET` | `/api` | Service info |
| 2 | `GET` | `/api/hello` | Hello World |
| 3 | `POST` | `/api/auth/login` | Login |
| 4 | `POST` | `/api/auth/refresh-token` | Refresh JWT |
| 5 | `GET` | `/actuator/health` | Health check |
| 6 | `GET` | `/actuator/health/readiness` | Readiness probe |

---

### 3.1 Auth — `/api/auth`

#### `POST /api/auth/login`

Login with mobile number and password.

**Request:**
```json
{
  "mobile": "9999999999",
  "password": "admin123"
}
```

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "OWNER",
  "userName": "Admin",
  "forcePasswordChange": true,
  "stores": [
    { "id": 1, "name": "Default Store", "city": null },
    { "id": 2, "name": "MG Road Branch", "city": "New Delhi" }
  ]
}
```

#### `POST /api/auth/change-password` 🔒

**Request:**
```json
{
  "currentPassword": "admin123",
  "newPassword": "MyNewSecure@123"
}
```

**Response `200 OK`:**
```json
{ "message": "Password changed successfully" }
```

#### `POST /api/auth/refresh-token`

**Request:**
```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

**Response `200 OK`:** Same structure as login response.

---

### 3.2 Staff — `/api/staff`

> Permission: `MANAGE_STAFF` (OWNER/ADMIN bypass)

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | `GET` | `/api/staff` | List all staff in organization |
| 2 | `GET` | `/api/staff/{id}` | Get staff member by ID |
| 3 | `POST` | `/api/staff` | Create new staff/admin |
| 4 | `PUT` | `/api/staff/{id}` | Update staff details |
| 5 | `DELETE` | `/api/staff/{id}` | Deactivate staff (soft delete) |

#### `POST /api/staff` — Create Staff

**Request:**
```json
{
  "name": "Arjun Kapoor",
  "mobile": "+919988776655",
  "password": "initial123",
  "role": "STAFF",
  "storeIds": [1],
  "permissions": ["VIEW_INVENTORY", "MANAGE_INVENTORY", "VIEW_CUSTOMERS"]
}
```

**Response `201 Created`:**
```json
{
  "id": 7,
  "name": "Arjun Kapoor",
  "mobile": "+919988776655",
  "email": null,
  "role": "STAFF",
  "salary": null,
  "commission": null,
  "salesTarget": null,
  "active": true,
  "stores": ["Default Store"],
  "permissions": ["VIEW_INVENTORY", "MANAGE_INVENTORY", "VIEW_CUSTOMERS"],
  "createdAt": "2026-03-14T10:00:00Z"
}
```

#### `PUT /api/staff/{id}` — Update Staff

**Request:**
```json
{
  "name": "Arjun Kapoor",
  "mobile": "+919988776655",
  "email": "arjun@jewelerp.com",
  "role": "ADMIN",
  "salary": 35000.00,
  "commission": 1.50,
  "salesTarget": 800000.00,
  "status": "ACTIVE",
  "storeIds": [1, 2],
  "permissions": ["VIEW_INVENTORY", "MANAGE_INVENTORY", "VIEW_CUSTOMERS", "MANAGE_CUSTOMERS"]
}
```

#### `DELETE /api/staff/{id}` — Response `204 No Content`

---

### 3.3 Categories — `/api/categories`

> Permission: `VIEW_INVENTORY` (read) / `MANAGE_INVENTORY` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/categories` | VIEW_INVENTORY | List all (current store) |
| 2 | `GET` | `/api/categories/{id}` | VIEW_INVENTORY | Get by ID |
| 3 | `POST` | `/api/categories` | MANAGE_INVENTORY | Create |
| 4 | `PUT` | `/api/categories/{id}` | MANAGE_INVENTORY | Update |
| 5 | `DELETE` | `/api/categories/{id}` | MANAGE_INVENTORY | Soft delete |

**Category Object:**
```json
{
  "id": 1,
  "active": true,
  "createdAt": "2026-03-08T12:24:24Z",
  "updatedAt": "2026-03-08T12:24:24Z",
  "name": "Rings",
  "description": "All types of rings"
}
```

**POST/PUT Request Body:**
```json
{
  "name": "Watches",
  "description": "Wrist watches and pocket watches"
}
```

---

### 3.4 Metal Types — `/api/metal-types`

> Permission: `VIEW_INVENTORY` (read) / `MANAGE_INVENTORY` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/metal-types` | VIEW_INVENTORY | List all (current store) |
| 2 | `GET` | `/api/metal-types/{id}` | VIEW_INVENTORY | Get by ID |
| 3 | `GET` | `/api/metal-types/search?name=Gold` | VIEW_INVENTORY | Search by metal name |
| 4 | `POST` | `/api/metal-types` | MANAGE_INVENTORY | Create |
| 5 | `PUT` | `/api/metal-types/{id}` | MANAGE_INVENTORY | Update |
| 6 | `DELETE` | `/api/metal-types/{id}` | MANAGE_INVENTORY | Soft delete |

**MetalType Object:**
```json
{
  "id": 2,
  "active": true,
  "createdAt": "2026-03-08T12:24:24Z",
  "updatedAt": "2026-03-08T12:24:24Z",
  "name": "Gold",
  "purity": "22K",
  "currentRate": 6900.00,
  "unit": "gram"
}
```

---

### 3.5 Jewelry Items — `/api/jewelry-items`

> Permission: `VIEW_INVENTORY` (read) / `MANAGE_INVENTORY` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/jewelry-items` | VIEW_INVENTORY | List all items |
| 2 | `GET` | `/api/jewelry-items/{id}` | VIEW_INVENTORY | Get by ID |
| 3 | `GET` | `/api/jewelry-items/sku/{sku}` | VIEW_INVENTORY | Get by SKU |
| 4 | `GET` | `/api/jewelry-items/category/{categoryId}` | VIEW_INVENTORY | Filter by category |
| 5 | `GET` | `/api/jewelry-items/status/{status}` | VIEW_INVENTORY | Filter by status |
| 6 | `POST` | `/api/jewelry-items` | MANAGE_INVENTORY | Create |
| 7 | `PUT` | `/api/jewelry-items/{id}` | MANAGE_INVENTORY | Update |
| 8 | `DELETE` | `/api/jewelry-items/{id}` | MANAGE_INVENTORY | Soft delete |

**Status Enum Values:** `IN_STOCK` | `SOLD` | `ON_APPROVAL` | `RETURNED` | `DAMAGED`

**JewelryItem Object:**
```json
{
  "id": 1,
  "active": true,
  "createdAt": "2026-03-08T12:24:24Z",
  "updatedAt": "2026-03-08T12:24:24Z",
  "sku": "RNG-G22-001",
  "name": "Classic Gold Band",
  "description": "Simple 22K gold band ring",
  "categoryId": 1,
  "metalTypeId": 2,
  "grossWeight": 5.200,
  "netWeight": 5.000,
  "makingCharges": 1500.00,
  "stoneCharges": 0.00,
  "otherCharges": 0.00,
  "quantity": 3,
  "hsnCode": "7113",
  "barcode": null,
  "status": "IN_STOCK"
}
```

---

### 3.6 Customers — `/api/customers`

> Permission: `VIEW_CUSTOMERS` (read) / `MANAGE_CUSTOMERS` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/customers` | VIEW_CUSTOMERS | List all |
| 2 | `GET` | `/api/customers/{id}` | VIEW_CUSTOMERS | Get by ID |
| 3 | `GET` | `/api/customers/phone/{phone}` | VIEW_CUSTOMERS | Get by phone |
| 4 | `GET` | `/api/customers/search?name=Rajesh` | VIEW_CUSTOMERS | Search by name |
| 5 | `POST` | `/api/customers` | MANAGE_CUSTOMERS | Create |
| 6 | `PUT` | `/api/customers/{id}` | MANAGE_CUSTOMERS | Update |
| 7 | `DELETE` | `/api/customers/{id}` | MANAGE_CUSTOMERS | Soft delete |

**Customer Object:**
```json
{
  "id": 1,
  "active": true,
  "createdAt": "2026-03-08T12:24:24Z",
  "updatedAt": "2026-03-08T12:24:24Z",
  "firstName": "Rajesh",
  "lastName": "Sharma",
  "phone": "9876543210",
  "email": "rajesh.sharma@email.com",
  "addressLine1": "12 MG Road",
  "addressLine2": null,
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001",
  "gstin": null,
  "pan": null
}
```

---

### 3.7 Invoices — `/api/invoices`

> Permission: `VIEW_BILLING` (read) / `MANAGE_BILLING` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/invoices` | VIEW_BILLING | List invoices (filterable) |
| 2 | `GET` | `/api/invoices/{id}` | VIEW_BILLING | Get invoice with full details |
| 3 | `POST` | `/api/invoices` | MANAGE_BILLING | Create new invoice |
| 4 | `PUT` | `/api/invoices/{id}` | MANAGE_BILLING | Update invoice |
| 5 | `PATCH` | `/api/invoices/{id}/status` | MANAGE_BILLING | Update status only |
| 6 | `POST` | `/api/invoices/{id}/payments` | MANAGE_BILLING | Record a split payment |

**GET `/api/invoices` — Query Parameters:**

| Param | Type | Example | Description |
|-------|------|---------|-------------|
| `status` | String | `CONFIRMED` | `DRAFT`, `CONFIRMED`, `CANCELLED` |
| `paymentStatus` | String | `UNPAID` | `UNPAID`, `PARTIAL`, `PAID` |
| `from` | Date | `2026-03-01` | Start date (ISO) |
| `to` | Date | `2026-03-31` | End date (ISO) |

#### `POST /api/invoices` — Create Invoice

**Request:**
```json
{
  "customerId": 1,
  "date": "2026-03-14",
  "items": [
    {
      "jewelryItemId": 1,
      "name": "Classic Gold Band",
      "weight": 5.000,
      "purity": "22K",
      "rate": 6900.00,
      "makingCharge": 12.0,
      "makingChargeType": "PERCENTAGE",
      "wastage": 2.0,
      "amount": 39468.00
    }
  ],
  "subtotal": 39468.00,
  "gstRate": 3.00,
  "gstAmount": 1184.04,
  "discount": 0.00,
  "roundOff": -2.04,
  "total": 40650.00,
  "oldGoldAdjustment": 0.00,
  "paymentMode": "UPI",
  "splitPayments": [
    { "mode": "UPI", "amount": 40650.00, "reference": "TXN123456" }
  ],
  "dueDate": null,
  "notes": "",
  "digitalSignature": "Admin",
  "status": "CONFIRMED"
}
```

**Response `201 Created`:**
```json
{
  "id": 1,
  "invoiceNumber": "INV-00001",
  "customerId": 1,
  "customer": "Rajesh Sharma",
  "storeId": 1,
  "type": "INVOICE",
  "date": "2026-03-14",
  "items": [
    {
      "id": 1,
      "jewelryItemId": 1,
      "name": "Classic Gold Band",
      "weight": 5.000,
      "purity": "22K",
      "rate": 6900.00,
      "makingCharge": 12.0,
      "makingChargeType": "PERCENTAGE",
      "wastage": 2.0,
      "amount": 39468.00
    }
  ],
  "subtotal": 39468.00,
  "gstRate": 3.00,
  "gstAmount": 1184.04,
  "discount": 0.00,
  "roundOff": -2.04,
  "total": 40650.00,
  "oldGoldAdjustment": 0.00,
  "paymentMode": "UPI",
  "splitPayments": [
    { "id": 1, "mode": "UPI", "amount": 40650.00, "reference": "TXN123456", "date": "2026-03-14" }
  ],
  "dueDate": null,
  "status": "CONFIRMED",
  "paymentStatus": "PAID",
  "paidAmount": 40650.00,
  "notes": "",
  "digitalSignature": "Admin",
  "createdAt": "2026-03-14T10:00:00Z"
}
```

#### `PATCH /api/invoices/{id}/status`

**Request:**
```json
{ "status": "CANCELLED" }
```

#### `POST /api/invoices/{id}/payments` — Record Payment

**Request:**
```json
{
  "mode": "CASH",
  "amount": 20000.00,
  "reference": null
}
```

**Payment Modes:** `CASH` | `UPI` | `CARD` | `BANK_TRANSFER`

---

### 3.8 Estimates — `/api/estimates`

> Permission: `VIEW_BILLING` (read) / `MANAGE_BILLING` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/estimates` | VIEW_BILLING | List all estimates |
| 2 | `GET` | `/api/estimates/{id}` | VIEW_BILLING | Get estimate detail |
| 3 | `POST` | `/api/estimates` | MANAGE_BILLING | Create estimate |
| 4 | `PUT` | `/api/estimates/{id}` | MANAGE_BILLING | Update estimate |
| 5 | `POST` | `/api/estimates/{id}/convert` | MANAGE_BILLING | Convert estimate → invoice |

**Request body (POST/PUT):** Same as `InvoiceRequest` (see section 3.7).

**Estimate Statuses:** `DRAFT` | `SENT` | `ACCEPTED` | `EXPIRED` | `CONVERTED`

**Note:** When you call `POST /api/estimates/{id}/convert`, the estimate status changes to `CONVERTED` and a new invoice is created. The response is an `InvoiceResponse`.

---

### 3.9 Credit Notes — `/api/credit-notes`

> Permission: `VIEW_BILLING` (read) / `MANAGE_BILLING` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/credit-notes` | VIEW_BILLING | List all credit notes |
| 2 | `GET` | `/api/credit-notes/{id}` | VIEW_BILLING | Get credit note detail |
| 3 | `POST` | `/api/credit-notes` | MANAGE_BILLING | Create credit note |

#### `POST /api/credit-notes`

**Request:**
```json
{
  "customerId": 1,
  "originalInvoiceId": 1,
  "date": "2026-03-14",
  "items": [
    {
      "jewelryItemId": 1,
      "name": "Classic Gold Band",
      "weight": 5.000,
      "purity": "22K",
      "rate": 6900.00,
      "makingCharge": 12.0,
      "makingChargeType": "PERCENTAGE",
      "wastage": 2.0,
      "amount": 39468.00
    }
  ],
  "subtotal": 39468.00,
  "gstRate": 3.00,
  "gstAmount": 1184.04,
  "discount": 0.00,
  "roundOff": -2.04,
  "total": 40650.00,
  "returnReason": "Customer returned — size mismatch",
  "refundMode": "CASH_REFUND",
  "notes": ""
}
```

**Refund Modes:** `SAME_MODE` | `STORE_CREDIT` | `CASH_REFUND` | `BANK_TRANSFER`

**Credit Note Statuses:** `DRAFT` | `ISSUED` | `APPLIED`

---

### 3.10 Ledger — `/api/ledger`

> Permission: `VIEW_ACCOUNTS` (read) / `MANAGE_ACCOUNTS` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/ledger` | VIEW_ACCOUNTS | List entries (filterable) |
| 2 | `GET` | `/api/ledger/{id}` | VIEW_ACCOUNTS | Get single entry |
| 3 | `POST` | `/api/ledger` | MANAGE_ACCOUNTS | Create entry |

**GET Query Parameters:** `type` (`CR`/`DR`), `from` (date), `to` (date)

#### `POST /api/ledger`

**Request:**
```json
{
  "date": "2026-03-14",
  "party": "Priya Sharma",
  "type": "CR",
  "amount": 40650.00,
  "mode": "UPI",
  "note": "Invoice #INV-001 — Full payment",
  "category": "Sales",
  "referenceId": "INV-001",
  "referenceType": "INVOICE"
}
```

**Type values:** `CR` (credit/money in) | `DR` (debit/money out)

**Reference Types:** `INVOICE` | `CREDIT_NOTE` | `EXPENSE` | `OLD_GOLD`

---

### 3.11 Expenses — `/api/expenses`

> Permission: `VIEW_ACCOUNTS` (read) / `MANAGE_ACCOUNTS` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/expenses` | VIEW_ACCOUNTS | List expenses (filterable) |
| 2 | `GET` | `/api/expenses/{id}` | VIEW_ACCOUNTS | Get single expense |
| 3 | `POST` | `/api/expenses` | MANAGE_ACCOUNTS | Create expense |
| 4 | `PUT` | `/api/expenses/{id}` | MANAGE_ACCOUNTS | Update expense |
| 5 | `DELETE` | `/api/expenses/{id}` | MANAGE_ACCOUNTS | Delete expense |

**GET Query Parameters:** `category`, `from` (date), `to` (date)

#### `POST /api/expenses`

**Request:**
```json
{
  "date": "2026-03-14",
  "category": "Salary",
  "description": "Staff salaries — March",
  "amount": 85000.00,
  "mode": "Bank Transfer"
}
```

**Expense Categories:** `Salary`, `Rent`, `Utilities`, `Insurance`, `Maintenance`, `Marketing`, `Repairs`, `Travel`, `Office Supplies`, `Miscellaneous`

---

### 3.12 Cash Register — `/api/cash-register`

> Permission: `VIEW_ACCOUNTS` (read) / `MANAGE_ACCOUNTS` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/cash-register` | VIEW_ACCOUNTS | List all register entries |
| 2 | `GET` | `/api/cash-register/current` | VIEW_ACCOUNTS | Get today's open register |
| 3 | `POST` | `/api/cash-register/open` | MANAGE_ACCOUNTS | Open register |
| 4 | `PATCH` | `/api/cash-register/{id}/close` | MANAGE_ACCOUNTS | Close register |

#### `POST /api/cash-register/open`

**Request:**
```json
{ "openingBalance": 50000.00 }
```

#### `PATCH /api/cash-register/{id}/close`

**Request:**
```json
{ "closingBalance": 78500.00 }
```

**Response:**
```json
{
  "id": 1,
  "storeId": 1,
  "registerDate": "2026-03-14",
  "openingBalance": 50000.00,
  "cashIn": 45000.00,
  "cashOut": 16500.00,
  "closingBalance": 78500.00,
  "openedBy": 1,
  "closedBy": 1,
  "status": "CLOSED",
  "createdAt": "2026-03-14T09:00:00Z",
  "updatedAt": "2026-03-14T18:00:00Z"
}
```

**Register Statuses:** `OPEN` | `CLOSED`

> **Note:** `GET /api/cash-register/current` returns `204 No Content` if no register is open today.

---

### 3.13 Suppliers — `/api/suppliers`

> Permission: `VIEW_ACCOUNTS` (read) / `MANAGE_ACCOUNTS` (write)

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/suppliers` | VIEW_ACCOUNTS | List all suppliers |
| 2 | `GET` | `/api/suppliers/{id}` | VIEW_ACCOUNTS | Get supplier detail |
| 3 | `POST` | `/api/suppliers` | MANAGE_ACCOUNTS | Create supplier |
| 4 | `PUT` | `/api/suppliers/{id}` | MANAGE_ACCOUNTS | Update supplier |
| 5 | `DELETE` | `/api/suppliers/{id}` | MANAGE_ACCOUNTS | Soft-delete supplier |

#### `POST /api/suppliers`

**Request:**
```json
{
  "name": "Gold Supplier Pvt Ltd",
  "phone": "+919988711223",
  "email": "gold@supplier.com",
  "city": "Mumbai",
  "address": "Zaveri Bazaar, Mumbai",
  "gst": "27AABCS1234F1Z5",
  "metals": ["Gold", "Silver"],
  "status": "ACTIVE"
}
```

**Supplier Statuses:** `ACTIVE` | `INACTIVE`

---

### 3.14 Daily Rates — `/api/rates`

> Permission: `MANAGE_RATES` (write only) — all authenticated users can read

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/rates` | Authenticated | Get current day's rates |
| 2 | `PUT` | `/api/rates` | MANAGE_RATES | Update today's rates |
| 3 | `GET` | `/api/rates/history?days=30` | Authenticated | Rate history (last N days) |
| 4 | `POST` | `/api/rates/alerts` | MANAGE_RATES | Create rate alert |
| 5 | `GET` | `/api/rates/alerts` | Authenticated | Get active alerts |

#### `PUT /api/rates` — Update Rates

**Request (all values per gram in INR):**
```json
{
  "gold24k": 7350.00,
  "gold22k": 6285.00,
  "gold18k": 5140.00,
  "gold14k": 4010.00,
  "silver": 92.00,
  "platinum": 3150.00,
  "rhodium": 13500.00,
  "roseGold18k": 5200.00,
  "whiteGold18k": 5300.00
}
```

#### `POST /api/rates/alerts`

**Request:**
```json
{
  "metal": "Gold",
  "condition": "ABOVE",
  "threshold": 8000.00,
  "customerId": 1
}
```

**Alert Conditions:** `ABOVE` | `BELOW`

> **Note:** `GET /api/rates` returns `204 No Content` if no rates have been set for today.

---

### 3.15 Old Gold — `/api/old-gold`

> Permission: `MANAGE_OLD_GOLD`

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/old-gold` | MANAGE_OLD_GOLD | List old gold purchases |
| 2 | `GET` | `/api/old-gold/{id}` | MANAGE_OLD_GOLD | Get purchase with purity test details |
| 3 | `POST` | `/api/old-gold` | MANAGE_OLD_GOLD | Record new purchase/exchange |
| 4 | `PUT` | `/api/old-gold/{id}` | MANAGE_OLD_GOLD | Update (add melting record, notes) |

#### `POST /api/old-gold`

**Request:**
```json
{
  "customerId": 1,
  "date": "2026-03-14",
  "weight": 15.200,
  "purity": "22K",
  "rate": 6150.00,
  "total": 93480.00,
  "type": "EXCHANGE",
  "kycDone": true,
  "notes": "Old bangles exchanged for new necklace",
  "purityTest": {
    "method": "XRF",
    "actualPurity": "21.8K",
    "purityPercent": 90.80,
    "testedBy": "Arjun Kapoor"
  },
  "meltingRecord": null
}
```

**Type values:** `PURCHASE` | `EXCHANGE`

**Purity Test Methods:** `XRF` | `Acid Test` | `Fire Assay`

#### `PUT /api/old-gold/{id}` — Add Melting Record

**Request (same structure, with meltingRecord):**
```json
{
  "customerId": 1,
  "date": "2026-03-14",
  "weight": 15.200,
  "purity": "22K",
  "rate": 6150.00,
  "total": 93480.00,
  "type": "EXCHANGE",
  "kycDone": true,
  "notes": "Old bangles exchanged for new necklace",
  "purityTest": null,
  "meltingRecord": {
    "meltedWeight": 14.800,
    "meltDate": "2026-03-15",
    "meltedBy": "Workshop Team"
  }
}
```

---

### 3.16 Saving Schemes — `/api/schemes`

> Permission: `MANAGE_SCHEMES`

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/schemes` | MANAGE_SCHEMES | List all schemes |
| 2 | `GET` | `/api/schemes/{id}` | MANAGE_SCHEMES | Get scheme with details |
| 3 | `POST` | `/api/schemes` | MANAGE_SCHEMES | Create new scheme |
| 4 | `PUT` | `/api/schemes/{id}` | MANAGE_SCHEMES | Update scheme |
| 5 | `GET` | `/api/schemes/{id}/members` | MANAGE_SCHEMES | List members of a scheme |
| 6 | `POST` | `/api/schemes/{id}/members` | MANAGE_SCHEMES | Add member to scheme |
| 7 | `POST` | `/api/schemes/{id}/members/{memberId}/payments` | MANAGE_SCHEMES | Record monthly payment |
| 8 | `GET` | `/api/schemes/{id}/members/{memberId}/payments` | MANAGE_SCHEMES | Get payment history |

#### `POST /api/schemes`

**Request:**
```json
{
  "name": "Gold Savings Plan",
  "durationMonths": 11,
  "monthlyAmount": 5000.00,
  "status": "ACTIVE",
  "startDate": "2025-06-01",
  "endDate": "2026-05-01",
  "bonusMonth": true,
  "description": "Pay ₹5,000/month for 11 months, get 12th month free."
}
```

**Scheme Statuses:** `ACTIVE` | `CLOSED` | `PAUSED`

#### `POST /api/schemes/{id}/members`

**Request:**
```json
{
  "customerId": 1,
  "name": "Rajesh Sharma",
  "phone": "9876543210",
  "joinDate": "2025-06-15"
}
```

**Member Statuses:** `ACTIVE` | `COMPLETED` | `DROPPED`

#### `POST /api/schemes/{id}/members/{memberId}/payments`

**Request:**
```json
{
  "month": 1,
  "amount": 5000.00,
  "date": "2025-07-01"
}
```

**Payment Statuses:** `PAID` | `PENDING` | `LATE`

---

### 3.17 Dashboard — `/api/dashboard`

> Permission: `VIEW_REPORTS`

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/dashboard/summary` | VIEW_REPORTS | Aggregated dashboard data |

**Response `200 OK`:**
```json
{
  "todaySales": 245000.00,
  "monthSales": 3850000.00,
  "totalInventory": 1250,
  "inventoryValue": 85000000.00,
  "pendingInvoices": 5,
  "totalCustomers": 180,
  "todayTransactions": 12,
  "topSellingCategory": "Necklaces",
  "revenueThisMonth": [
    { "date": "2026-03-01", "amount": 125000.00 },
    { "date": "2026-03-02", "amount": 340000.00 },
    { "date": "2026-03-03", "amount": 0.00 }
  ]
}
```

---

### 3.18 Settings — `/api/settings`

> Owner only for writes

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/settings` | Authenticated | Get all organization settings |
| 2 | `PUT` | `/api/settings` | MANAGE_SETTINGS | Update settings (Owner only) |
| 3 | `GET` | `/api/settings/expense-categories` | Authenticated | Get expense category list |

#### `GET /api/settings` — Response

```json
{
  "businessName": "Rajmahal Jewellers",
  "gstin": "08AAECR1234F1Z5",
  "phone": "+91 141 456 7890",
  "email": "info@rajmahaljewellers.com",
  "invoicePrefix": "INV",
  "defaultMakingCharge": "12",
  "gstRate": "3",
  "defaultWastage": "2",
  "currency": "INR",
  "weightUnit": "grams"
}
```

#### `PUT /api/settings` — Request

```json
{
  "businessName": "Rajmahal Jewellers",
  "gstin": "08AAECR1234F1Z5",
  "invoicePrefix": "INV",
  "gstRate": "3"
}
```

> **Note:** Settings are key-value pairs. Send any keys you want to set/update.

#### `GET /api/settings/expense-categories` — Response

```json
["Salary", "Rent", "Utilities", "Insurance", "Maintenance", "Marketing", "Repairs", "Travel", "Office Supplies", "Miscellaneous"]
```

---

### 3.19 Enquiries (Admin) — `/api/enquiries`

> View and respond to customer enquiries submitted via the Customer App. Includes customer contact details, enquiry image, and response tracking.

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/enquiries` | VIEW_CUSTOMERS | List all enquiries for the current store |
| 2 | `GET` | `/api/enquiries/{id}` | VIEW_CUSTOMERS | Get single enquiry with full details |
| 3 | `PUT` | `/api/enquiries/{id}/respond` | MANAGE_CUSTOMERS | Respond to a customer enquiry |
| 4 | `PATCH` | `/api/enquiries/{id}/close` | MANAGE_CUSTOMERS | Close an enquiry |

**GET Response Item:**
```json
{
  "id": 5,
  "customerId": 12,
  "customerName": "Priya Patel",
  "customerPhone": "9876543210",
  "customerEmail": "priya@email.com",
  "jewelryItemId": 2,
  "jewelryItemName": "Diamond Solitaire Ring",
  "jewelryItemSku": "RNG-G22-001",
  "subject": "Available in size 7?",
  "message": "Hi, is this ring available in size 7? Attaching a reference image.",
  "imageUrl": "https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com/enquiries/844732ce.png",
  "status": "OPEN",
  "adminResponse": null,
  "respondedBy": null,
  "respondedAt": null,
  "createdAt": "2026-03-19T10:30:00Z",
  "updatedAt": "2026-03-19T10:30:00Z"
}
```

**PUT `/api/enquiries/{id}/respond` Request:**
```json
{
  "adminResponse": "Yes, size 7 is available. Visit us this weekend!",
  "status": "RESPONDED"
}
```

**Status values:** `OPEN`, `RESPONDED`, `CLOSED`

> **Note:** `customerPhone` is provided so the admin can directly call or WhatsApp the customer from the Desktop/Admin app. The `imageUrl` (if present) shows the reference image the customer attached.

---

### 3.20 Activity Logs — `/api/activity-logs`

> Auto-created by the backend on create/update operations. Read-only for frontend.

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/activity-logs` | Authenticated | List activity logs (filterable) |

**GET Query Parameters:** `module`, `from` (date), `to` (date)

**Response Item:**
```json
{
  "id": 1,
  "storeId": 1,
  "userId": 1,
  "userName": "Admin",
  "action": "Created Invoice",
  "detail": "Invoice #INV-001 for Rajesh Sharma — ₹40,650",
  "module": "Billing",
  "entityType": "INVOICE",
  "entityId": 1,
  "createdAt": "2026-03-14T10:30:15Z"
}
```

**Module values:** `Billing`, `Inventory`, `Staff`, `Customers`, `Accounts`, `Rates`, `OldGold`, `Schemes`

---

### 3.21 DB Admin — `/api/db`

> Internal/debug endpoints

| # | Method | Endpoint | Permission | Description |
|---|--------|----------|------------|-------------|
| 1 | `GET` | `/api/db/status` | VIEW_REPORTS | DB status & table row counts |
| 2 | `GET` | `/api/db/users` | MANAGE_STAFF | Raw user list |
| 3 | `GET` | `/api/db/users/access` | MANAGE_STAFF | User → store access mapping |
| 4 | `GET` | `/api/db/users/permissions` | MANAGE_STAFF | User → store → permissions |
| 5 | `GET` | `/api/db/stores` | VIEW_REPORTS | Raw store list |

---

### 3.21 Health / Hello

| # | Method | Endpoint | Auth | Description |
|---|--------|----------|------|-------------|
| 1 | `GET` | `/api` | Public | Service welcome message |
| 2 | `GET` | `/api/hello` | Public | Hello world check |
| 3 | `GET` | `/actuator/health` | Public | Spring Boot health |
| 4 | `GET` | `/actuator/health/readiness` | Public | Readiness probe (ALB) |
| 5 | `GET` | `/actuator/health/liveness` | Public | Liveness probe |

**`GET /api/hello` — Response:**
```json
{
  "message": "Hello, World! JewelERP is running.",
  "service": "jewel-erp",
  "version": "0.0.1",
  "timestamp": "2026-03-14T10:00:00Z",
  "status": "UP"
}
```

---

## 4. Database Schema — All Tables & Columns

> **33 tables** across 6 Flyway migrations (V1 through V6).
> Engine: MySQL 8.0 / InnoDB / utf8mb4_unicode_ci

---

### 4.1 Core / Multi-Tenant

#### `organizations`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| name | VARCHAR(200) | NO | | | Business name |
| active | BOOLEAN | YES | TRUE | | Soft delete |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `stores`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| org_id | BIGINT | NO | | FK → organizations | |
| name | VARCHAR(200) | NO | | | Branch name |
| address | VARCHAR(500) | YES | | | |
| city | VARCHAR(100) | YES | | | |
| state | VARCHAR(100) | YES | | | |
| pincode | VARCHAR(10) | YES | | | |
| phone | VARCHAR(15) | YES | | | |
| gstin | VARCHAR(20) | YES | | | GST number |
| active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `users`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| org_id | BIGINT | NO | | FK → organizations | |
| name | VARCHAR(150) | NO | | | Display name |
| mobile | VARCHAR(15) | NO | | UQ | Login identifier |
| email | VARCHAR(150) | YES | | | |
| salary | DECIMAL(14,2) | YES | | | Monthly salary |
| commission | DECIMAL(5,2) | YES | | | Commission % |
| sales_target | DECIMAL(14,2) | YES | | | Monthly target |
| password_hash | VARCHAR(255) | NO | | | BCrypt hash |
| role | ENUM('OWNER','ADMIN','STAFF') | NO | | | |
| force_password_change | BOOLEAN | YES | TRUE | | |
| active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `user_store_access`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| user_id | BIGINT | NO | | FK → users | |
| store_id | BIGINT | NO | | FK → stores | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |

**Unique constraint:** `(user_id, store_id)`

#### `permissions`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| name | VARCHAR(50) | NO | | UQ | Permission code |
| description | VARCHAR(200) | YES | | | |

#### `user_permissions`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| user_id | BIGINT | NO | | FK → users | |
| store_id | BIGINT | NO | | FK → stores | Per-store |
| permission_id | BIGINT | NO | | FK → permissions | |

**Unique constraint:** `(user_id, store_id, permission_id)`

---

### 4.2 Inventory

#### `categories`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores | |
| name | VARCHAR(100) | NO | | UQ(name, store_id) | |
| description | VARCHAR(255) | YES | | | |
| active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `metal_types`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores | |
| name | VARCHAR(50) | NO | | | Gold, Silver, Platinum |
| purity | VARCHAR(20) | NO | | UQ(name,purity,store_id) | 24K, 22K, 925 |
| current_rate | DECIMAL(12,2) | NO | | | Rate per gram |
| unit | VARCHAR(10) | NO | 'gram' | | |
| active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `jewelry_items`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores | |
| sku | VARCHAR(50) | NO | | UQ(sku, store_id) | Stock keeping unit |
| name | VARCHAR(200) | NO | | | Item name |
| description | TEXT | YES | | | |
| category_id | BIGINT | NO | | FK → categories | |
| metal_type_id | BIGINT | NO | | FK → metal_types | |
| gross_weight | DECIMAL(10,3) | NO | | | Total weight (grams) |
| net_weight | DECIMAL(10,3) | NO | | | Metal weight (grams) |
| making_charges | DECIMAL(12,2) | NO | 0 | | |
| stone_charges | DECIMAL(12,2) | NO | 0 | | |
| other_charges | DECIMAL(12,2) | NO | 0 | | |
| quantity | INT | NO | 1 | | |
| hsn_code | VARCHAR(20) | YES | | | HSN for GST |
| barcode | VARCHAR(100) | YES | | | |
| status | ENUM | YES | 'IN_STOCK' | IDX | IN_STOCK, SOLD, ON_APPROVAL, RETURNED, DAMAGED |
| active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `customers`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores | |
| first_name | VARCHAR(100) | NO | | IDX | |
| last_name | VARCHAR(100) | YES | | IDX | |
| phone | VARCHAR(15) | NO | | UQ(phone, store_id) | |
| email | VARCHAR(150) | YES | | | |
| address_line1 | VARCHAR(255) | YES | | | |
| address_line2 | VARCHAR(255) | YES | | | |
| city | VARCHAR(100) | YES | | | |
| state | VARCHAR(100) | YES | | | |
| pincode | VARCHAR(10) | YES | | | |
| gstin | VARCHAR(20) | YES | | | B2B GST number |
| pan | VARCHAR(15) | YES | | | PAN card |
| active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

---

### 4.3 Billing

#### `invoices`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| invoice_type | ENUM('INVOICE','ESTIMATE','CREDIT_NOTE') | YES | 'INVOICE' | | |
| invoice_number | VARCHAR(30) | NO | | UQ(inv_num, store_id), IDX | Auto-generated |
| customer_id | BIGINT | NO | | FK → customers, IDX | |
| invoice_date | DATE | NO | | IDX | |
| subtotal | DECIMAL(14,2) | NO | 0 | | |
| discount | DECIMAL(14,2) | NO | 0 | | |
| gst_rate | DECIMAL(5,2) | NO | 3.00 | | GST percentage |
| gst_amount | DECIMAL(14,2) | NO | 0 | | |
| round_off | DECIMAL(10,2) | NO | 0 | | |
| old_gold_adjustment | DECIMAL(14,2) | NO | 0 | | |
| cgst_amount | DECIMAL(14,2) | NO | 0 | | |
| sgst_amount | DECIMAL(14,2) | NO | 0 | | |
| igst_amount | DECIMAL(14,2) | NO | 0 | | |
| total_amount | DECIMAL(14,2) | NO | 0 | | |
| paid_amount | DECIMAL(14,2) | NO | 0 | | |
| payment_status | ENUM('UNPAID','PARTIAL','PAID') | YES | 'UNPAID' | | |
| payment_mode | ENUM('CASH','UPI','CARD','BANK_TRANSFER','MIXED') | YES | | | |
| due_date | DATE | YES | | | For credit sales |
| notes | TEXT | YES | | | |
| digital_signature | VARCHAR(200) | YES | | | Signer name |
| status | ENUM('DRAFT','CONFIRMED','CANCELLED') | YES | 'DRAFT' | IDX | |
| active | BOOLEAN | YES | TRUE | | |
| created_by | BIGINT | YES | | | User ID |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `invoice_items`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| invoice_id | BIGINT | NO | | FK → invoices, IDX | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| jewelry_item_id | BIGINT | NO | | FK → jewelry_items, IDX | |
| quantity | INT | NO | 1 | | |
| metal_rate | DECIMAL(12,2) | NO | | | Rate at sale time |
| metal_value | DECIMAL(14,2) | NO | | | |
| making_charges | DECIMAL(12,2) | NO | 0 | | |
| stone_charges | DECIMAL(12,2) | NO | 0 | | |
| other_charges | DECIMAL(12,2) | NO | 0 | | |
| discount | DECIMAL(12,2) | NO | 0 | | |
| taxable_amount | DECIMAL(14,2) | NO | | | |
| cgst_percent | DECIMAL(5,2) | NO | 1.50 | | |
| sgst_percent | DECIMAL(5,2) | NO | 1.50 | | |
| cgst_amount | DECIMAL(12,2) | NO | 0 | | |
| sgst_amount | DECIMAL(12,2) | NO | 0 | | |
| total_amount | DECIMAL(14,2) | NO | | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |

#### `invoice_payments`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| invoice_id | BIGINT | NO | | FK → invoices, IDX | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| mode | VARCHAR(30) | NO | | | CASH, UPI, CARD, BANK_TRANSFER |
| amount | DECIMAL(14,2) | NO | | | |
| reference | VARCHAR(100) | YES | | | Txn reference |
| payment_date | DATE | NO | | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |

#### `estimates`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| estimate_number | VARCHAR(30) | NO | | UQ(est_num, store_id) | |
| customer_id | BIGINT | NO | | FK → customers, IDX | |
| estimate_date | DATE | NO | | IDX | |
| subtotal | DECIMAL(14,2) | NO | 0 | | |
| discount | DECIMAL(14,2) | NO | 0 | | |
| gst_rate | DECIMAL(5,2) | NO | 3.00 | | |
| gst_amount | DECIMAL(14,2) | NO | 0 | | |
| round_off | DECIMAL(10,2) | NO | 0 | | |
| total_amount | DECIMAL(14,2) | NO | 0 | | |
| notes | TEXT | YES | | | |
| digital_signature | VARCHAR(200) | YES | | | |
| status | ENUM | YES | 'DRAFT' | | DRAFT, SENT, ACCEPTED, EXPIRED, CONVERTED |
| converted_invoice_id | BIGINT | YES | | | Invoice ID after conversion |
| valid_until | DATE | YES | | | Expiry date |
| active | BOOLEAN | YES | TRUE | | |
| created_by | BIGINT | YES | | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `estimate_items`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| estimate_id | BIGINT | NO | | FK → estimates, IDX | |
| store_id | BIGINT | NO | | FK → stores | |
| jewelry_item_id | BIGINT | YES | | | Optional link |
| name | VARCHAR(200) | NO | | | |
| weight | DECIMAL(10,3) | YES | | | |
| purity | VARCHAR(20) | YES | | | |
| rate | DECIMAL(12,2) | NO | 0 | | |
| making_charge | DECIMAL(12,2) | NO | 0 | | |
| making_charge_type | ENUM('PERCENTAGE','FLAT') | YES | 'PERCENTAGE' | | |
| wastage | DECIMAL(5,2) | NO | 0 | | |
| amount | DECIMAL(14,2) | NO | 0 | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |

#### `credit_notes`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| credit_note_number | VARCHAR(30) | NO | | UQ(cn_num, store_id) | |
| customer_id | BIGINT | NO | | FK → customers, IDX | |
| original_invoice_id | BIGINT | YES | | | Original invoice |
| credit_note_date | DATE | NO | | | |
| subtotal | DECIMAL(14,2) | NO | 0 | | |
| discount | DECIMAL(14,2) | NO | 0 | | |
| gst_rate | DECIMAL(5,2) | NO | 3.00 | | |
| gst_amount | DECIMAL(14,2) | NO | 0 | | |
| round_off | DECIMAL(10,2) | NO | 0 | | |
| total_amount | DECIMAL(14,2) | NO | 0 | | |
| return_reason | TEXT | YES | | | |
| refund_mode | ENUM | YES | 'SAME_MODE' | | SAME_MODE, STORE_CREDIT, CASH_REFUND, BANK_TRANSFER |
| notes | TEXT | YES | | | |
| status | ENUM | YES | 'DRAFT' | | DRAFT, ISSUED, APPLIED |
| active | BOOLEAN | YES | TRUE | | |
| created_by | BIGINT | YES | | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `credit_note_items`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| credit_note_id | BIGINT | NO | | FK → credit_notes, IDX | |
| store_id | BIGINT | NO | | FK → stores | |
| jewelry_item_id | BIGINT | YES | | | |
| name | VARCHAR(200) | NO | | | |
| weight | DECIMAL(10,3) | YES | | | |
| purity | VARCHAR(20) | YES | | | |
| rate | DECIMAL(12,2) | NO | 0 | | |
| making_charge | DECIMAL(12,2) | NO | 0 | | |
| making_charge_type | ENUM('PERCENTAGE','FLAT') | YES | 'PERCENTAGE' | | |
| wastage | DECIMAL(5,2) | NO | 0 | | |
| amount | DECIMAL(14,2) | NO | 0 | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |

---

### 4.4 Accounts

#### `ledger_entries`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| entry_date | DATE | NO | | IDX | |
| party | VARCHAR(200) | NO | | | Party name |
| type | ENUM('CR','DR') | NO | | IDX | Credit / Debit |
| amount | DECIMAL(14,2) | NO | | | |
| mode | VARCHAR(30) | NO | | | Payment mode |
| note | TEXT | YES | | | |
| category | VARCHAR(100) | YES | | | Sales, Purchase, etc. |
| reference_id | VARCHAR(50) | YES | | | Related entity ID |
| reference_type | VARCHAR(30) | YES | | | INVOICE, CREDIT_NOTE, EXPENSE, OLD_GOLD |
| created_by | BIGINT | YES | | | |
| active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `expenses`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| expense_date | DATE | NO | | IDX | |
| category | VARCHAR(100) | NO | | IDX | Expense category |
| description | TEXT | YES | | | |
| amount | DECIMAL(14,2) | NO | | | |
| mode | VARCHAR(30) | NO | | | Payment mode |
| created_by | BIGINT | YES | | | |
| active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `cash_registers`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, UQ(store_id, register_date) | |
| register_date | DATE | NO | | | |
| opening_balance | DECIMAL(14,2) | NO | 0 | | |
| cash_in | DECIMAL(14,2) | NO | 0 | | Auto-updated |
| cash_out | DECIMAL(14,2) | NO | 0 | | Auto-updated |
| closing_balance | DECIMAL(14,2) | YES | | | Manual on close |
| opened_by | BIGINT | YES | | | User ID |
| closed_by | BIGINT | YES | | | User ID |
| status | ENUM('OPEN','CLOSED') | YES | 'OPEN' | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

---

### 4.5 Suppliers

#### `suppliers`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| name | VARCHAR(200) | NO | | | |
| phone | VARCHAR(15) | YES | | | |
| email | VARCHAR(150) | YES | | | |
| city | VARCHAR(100) | YES | | | |
| address | VARCHAR(500) | YES | | | |
| gst | VARCHAR(20) | YES | | | GST number |
| status | ENUM('ACTIVE','INACTIVE') | YES | 'ACTIVE' | | |
| active | BOOLEAN | YES | TRUE | | Soft delete |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `supplier_metals`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| supplier_id | BIGINT | NO | | FK → suppliers (CASCADE DELETE), IDX | |
| metal | VARCHAR(50) | NO | | | Metal name |

---

### 4.6 Rates

#### `daily_rates`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, UQ(store_id, rate_date) | |
| rate_date | DATE | NO | | | |
| gold_24k | DECIMAL(12,2) | NO | 0 | | Per gram |
| gold_22k | DECIMAL(12,2) | NO | 0 | | |
| gold_18k | DECIMAL(12,2) | NO | 0 | | |
| gold_14k | DECIMAL(12,2) | NO | 0 | | |
| silver | DECIMAL(12,2) | NO | 0 | | |
| platinum | DECIMAL(12,2) | NO | 0 | | |
| rhodium | DECIMAL(12,2) | NO | 0 | | |
| rose_gold_18k | DECIMAL(12,2) | NO | 0 | | |
| white_gold_18k | DECIMAL(12,2) | NO | 0 | | |
| updated_by | VARCHAR(150) | YES | | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `rate_alerts`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| metal | VARCHAR(50) | NO | | | |
| condition_type | ENUM('ABOVE','BELOW') | NO | | | |
| threshold | DECIMAL(12,2) | NO | | | Price threshold |
| customer_id | BIGINT | YES | | FK → customers | |
| is_active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |

---

### 4.7 Old Gold

#### `old_gold_purchases`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| customer_id | BIGINT | NO | | FK → customers, IDX | |
| purchase_date | DATE | NO | | | |
| weight | DECIMAL(10,3) | NO | | | Grams |
| purity | VARCHAR(20) | NO | | | |
| rate | DECIMAL(12,2) | NO | | | Per gram |
| total | DECIMAL(14,2) | NO | | | |
| type | ENUM('PURCHASE','EXCHANGE') | YES | 'PURCHASE' | | |
| kyc_done | BOOLEAN | YES | FALSE | | |
| notes | TEXT | YES | | | |
| active | BOOLEAN | YES | TRUE | | |
| created_by | BIGINT | YES | | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `old_gold_purity_tests`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| old_gold_id | BIGINT | NO | | FK → old_gold_purchases, IDX | |
| method | VARCHAR(50) | NO | | | XRF, Acid Test, Fire Assay |
| actual_purity | VARCHAR(20) | YES | | | |
| purity_percent | DECIMAL(5,2) | YES | | | |
| tested_by | VARCHAR(150) | YES | | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |

#### `old_gold_melting_records`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| old_gold_id | BIGINT | NO | | FK → old_gold_purchases, IDX | |
| melted_weight | DECIMAL(10,3) | NO | | | |
| melt_date | DATE | NO | | | |
| melted_by | VARCHAR(150) | YES | | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |

---

### 4.8 Saving Schemes

#### `schemes`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| name | VARCHAR(200) | NO | | | |
| duration_months | INT | NO | | | |
| monthly_amount | DECIMAL(14,2) | NO | | | |
| status | ENUM('ACTIVE','CLOSED','PAUSED') | YES | 'ACTIVE' | | |
| start_date | DATE | NO | | | |
| end_date | DATE | NO | | | |
| bonus_month | BOOLEAN | YES | FALSE | | |
| description | TEXT | YES | | | |
| active | BOOLEAN | YES | TRUE | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `scheme_members`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| scheme_id | BIGINT | NO | | FK → schemes, IDX | |
| customer_id | BIGINT | YES | | FK → customers | |
| name | VARCHAR(200) | NO | | | |
| phone | VARCHAR(15) | YES | | | |
| join_date | DATE | NO | | | |
| status | ENUM('ACTIVE','COMPLETED','DROPPED') | YES | 'ACTIVE' | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |
| updated_at | TIMESTAMP | YES | ON UPDATE | | |

#### `scheme_payments`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| scheme_member_id | BIGINT | NO | | FK → scheme_members, IDX | |
| month_number | INT | NO | | | 1, 2, 3... |
| amount | DECIMAL(14,2) | NO | | | |
| payment_date | DATE | NO | | | |
| status | ENUM('PAID','PENDING','LATE') | YES | 'PAID' | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | | |

---

### 4.9 Settings & Logs

#### `org_settings`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| org_id | BIGINT | NO | | FK → organizations, UQ(org_id, setting_key) | |
| setting_key | VARCHAR(100) | NO | | | |
| setting_value | TEXT | YES | | | |

#### `activity_logs`

| Column | Type | Nullable | Default | Key | Description |
|--------|------|----------|---------|-----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK | |
| store_id | BIGINT | NO | | FK → stores, IDX | |
| user_id | BIGINT | YES | | | |
| user_name | VARCHAR(150) | YES | | | Denormalized |
| action | VARCHAR(200) | NO | | | |
| detail | TEXT | YES | | | |
| module | VARCHAR(50) | YES | | IDX | |
| entity_type | VARCHAR(50) | YES | | | |
| entity_id | BIGINT | YES | | | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | IDX | |

---

## 5. Entity Relationship Diagram (ERD)

```
                        ┌──────────────────┐
                        │  organizations   │
                        │──────────────────│
                        │ PK id            │
                        │    name          │
                        └────────┬─────────┘
                                 │ 1
                    ┌────────────┼────────────┐
                    │ ∞          │ ∞           │ ∞
             ┌──────┴──────┐  ┌─┴──────┐  ┌───┴────────┐
             │   stores    │  │ users  │  │org_settings│
             │─────────────│  │────────│  │────────────│
             │ PK id       │  │ PK id  │  │ FK org_id  │
             │ FK org_id   │  │FK org_id│ │ setting_key│
             │ name, city  │  │ mobile │  │ setting_val│
             │ gstin       │  │ role   │  └────────────┘
             └──────┬──────┘  │password│
                    │         └───┬────┘
                    │             │
         ┌──────────┤         ┌───┴────────────────┐
         │          │         │                    │
         │   ┌──────┴──────┐  │  ┌─────────────┐   │  ┌──────────────┐
         │   │user_store_  │──┘  │user_         │   │  │ permissions  │
         │   │access       │     │permissions   │───┘  │──────────────│
         │   │─────────────│     │─────────────│       │ PK id       │
         │   │ FK user_id  │     │ FK user_id  │───────│ name        │
         │   │ FK store_id │     │ FK store_id │       └──────────────┘
         │   └─────────────┘     │ FK perm_id  │
         │                       └─────────────┘
         │
   ┌─────┴───────────────────────────────────────────────────────────┐
   │                    ALL SCOPED BY store_id                       │
   │                                                                 │
   │  ┌────────────┐  ┌─────────────┐  ┌────────────────┐           │
   │  │ categories │  │ metal_types │  │   customers    │           │
   │  │────────────│  │─────────────│  │────────────────│           │
   │  │ PK id      │  │ PK id       │  │ PK id          │           │
   │  │ name       │  │ name, purity│  │ first_name     │           │
   │  │ description│  │ current_rate│  │ last_name      │           │
   │  └──────┬─────┘  └──────┬──────┘  │ phone, email   │           │
   │         │               │         │ city, state     │           │
   │         │   ┌───────────┘         └───────┬────────┘           │
   │         │   │                             │                     │
   │    ┌────┴───┴────┐                        │                     │
   │    │jewelry_items│          ┌─────────────┼──────────────┐      │
   │    │─────────────│          │             │              │      │
   │    │ PK id       │    ┌─────┴─────┐ ┌────┴────┐  ┌──────┴────┐ │
   │    │ FK cat_id   │    │ invoices  │ │estimates│  │credit_   │ │
   │    │ FK metal_id │    │───────────│ │─────────│  │notes     │ │
   │    │ sku, name   │    │ PK id     │ │ PK id   │  │──────────│ │
   │    │ weight      │    │ inv_num   │ │ est_num │  │ PK id    │ │
   │    │ status      │    │FK cust_id │ │FK cust  │  │ cn_num   │ │
   │    └─────────────┘    │ total_amt │ │ total   │  │FK cust   │ │
   │                       │ status    │ │ status  │  │ total    │ │
   │                       └─────┬─────┘ └────┬───┘  │ refund   │ │
   │                             │             │      └─────┬────┘ │
   │                        ┌────┴────┐   ┌────┴─────┐  ┌───┴─────┐│
   │                        │inv_items│   │est_items │  │cn_items ││
   │                        │inv_pay  │   └──────────┘  └─────────┘│
   │                        └─────────┘                            │
   │                                                               │
   │  ┌──────────────┐  ┌──────────┐  ┌────────────────┐          │
   │  │ledger_entries│  │ expenses │  │ cash_registers │          │
   │  │──────────────│  │──────────│  │────────────────│          │
   │  │ party, type  │  │ category │  │ opening_bal    │          │
   │  │ amount, mode │  │ amount   │  │ closing_bal    │          │
   │  └──────────────┘  └──────────┘  │ status         │          │
   │                                  └────────────────┘          │
   │                                                               │
   │  ┌───────────┐  ┌─────────────────┐  ┌───────────┐          │
   │  │ suppliers │  │old_gold_        │  │daily_rates│          │
   │  │───────────│  │purchases        │  │───────────│          │
   │  │ name,phone│  │─────────────────│  │ gold_24k  │          │
   │  │ gst       │  │ FK customer_id  │  │ gold_22k  │          │
   │  └─────┬─────┘  │ weight, purity  │  │ silver    │          │
   │        │        │ type (PUR/EXCH) │  │ platinum  │          │
   │  ┌─────┴──────┐ └──────┬──────────┘  └───────────┘          │
   │  │supplier_   │        │                                     │
   │  │metals      │  ┌─────┴──────┐  ┌────────────┐             │
   │  └────────────┘  │purity_tests│  │melting_recs│             │
   │                  └────────────┘  └────────────┘             │
   │                                                               │
   │  ┌──────────┐     ┌──────────────┐                           │
   │  │ schemes  │────→│scheme_members│────→┌───────────────┐     │
   │  │──────────│     │──────────────│     │scheme_payments│     │
   │  │ name     │     │ FK scheme_id │     │───────────────│     │
   │  │ monthly  │     │ FK customer  │     │ month_number  │     │
   │  │ duration │     │ name, phone  │     │ amount, date  │     │
   │  └──────────┘     └──────────────┘     └───────────────┘     │
   │                                                               │
   │  ┌──────────────┐  ┌─────────────┐                           │
   │  │activity_logs │  │ rate_alerts │                           │
   │  │──────────────│  │─────────────│                           │
   │  │ action       │  │ metal       │                           │
   │  │ detail       │  │ condition   │                           │
   │  │ module       │  │ threshold   │                           │
   │  └──────────────┘  └─────────────┘                           │
   └───────────────────────────────────────────────────────────────┘
```

### Relationship Summary

```
organizations  1 ──→ ∞  stores
organizations  1 ──→ ∞  users
organizations  1 ──→ ∞  org_settings

users  ∞ ←──→ ∞  stores          (via user_store_access)
users  ∞ ←──→ ∞  permissions     (via user_permissions, per store)

stores  1 ──→ ∞  categories
stores  1 ──→ ∞  metal_types
stores  1 ──→ ∞  jewelry_items
stores  1 ──→ ∞  customers
stores  1 ──→ ∞  invoices, invoice_items, invoice_payments
stores  1 ──→ ∞  estimates, estimate_items
stores  1 ──→ ∞  credit_notes, credit_note_items
stores  1 ──→ ∞  ledger_entries, expenses, cash_registers
stores  1 ──→ ∞  suppliers, daily_rates, rate_alerts
stores  1 ──→ ∞  old_gold_purchases, schemes, activity_logs

categories   1 ──→ ∞  jewelry_items
metal_types  1 ──→ ∞  jewelry_items

customers  1 ──→ ∞  invoices
customers  1 ──→ ∞  estimates
customers  1 ──→ ∞  credit_notes
customers  1 ──→ ∞  old_gold_purchases
customers  1 ──→ ∞  rate_alerts
customers  1 ──→ ∞  scheme_members

invoices       1 ──→ ∞  invoice_items
invoices       1 ──→ ∞  invoice_payments
estimates      1 ──→ ∞  estimate_items
credit_notes   1 ──→ ∞  credit_note_items
suppliers      1 ──→ ∞  supplier_metals
old_gold_purchases  1 ──→ ∞  old_gold_purity_tests
old_gold_purchases  1 ──→ ∞  old_gold_melting_records
schemes        1 ──→ ∞  scheme_members
scheme_members 1 ──→ ∞  scheme_payments
```

---

## 6. Permissions Master List

| ID | Code | Description | Protects |
|----|------|-------------|----------|
| 1 | `VIEW_INVENTORY` | View jewelry items, categories, metals | Categories, MetalTypes, JewelryItems (GET) |
| 2 | `MANAGE_INVENTORY` | Add/edit/delete inventory items | Categories, MetalTypes, JewelryItems (POST/PUT/DELETE) |
| 3 | `VIEW_CUSTOMERS` | View customer list | Customers (GET) |
| 4 | `MANAGE_CUSTOMERS` | Add/edit customers | Customers (POST/PUT/DELETE) |
| 5 | `CREATE_INVOICE` | Create new invoices/bills | _(legacy — use MANAGE_BILLING)_ |
| 6 | `VIEW_INVOICES` | View existing invoices | _(legacy — use VIEW_BILLING)_ |
| 7 | `MANAGE_INVOICES` | Edit/cancel invoices | _(legacy — use MANAGE_BILLING)_ |
| 8 | `VIEW_REPORTS` | View reports & dashboard | Dashboard, DB Admin (GET) |
| 9 | `MANAGE_STAFF` | Add/remove staff members | Staff, DB Admin (users) |
| 10 | `VIEW_BILLING` | View invoices, estimates, credit notes | Invoices, Estimates, CreditNotes (GET) |
| 11 | `MANAGE_BILLING` | Create/update billing documents | Invoices, Estimates, CreditNotes (POST/PUT/PATCH) |
| 12 | `VIEW_ACCOUNTS` | View ledger, expenses, suppliers, register | Ledger, Expenses, CashRegister, Suppliers (GET) |
| 13 | `MANAGE_ACCOUNTS` | Create/update accounting entries | Ledger, Expenses, CashRegister, Suppliers (POST/PUT/DELETE) |
| 14 | `MANAGE_RATES` | Update daily metal rates | Rates (PUT), Rate Alerts (POST) |
| 15 | `MANAGE_OLD_GOLD` | Record old gold purchases | Old Gold (all) |
| 16 | `MANAGE_SCHEMES` | Manage saving schemes | Schemes (all) |

> **Important:** `OWNER` and `ADMIN` roles bypass **all** permission checks. Permissions only apply to `STAFF`.

---

## 7. Seed Data / Test Users

### Pre-created Users

| # | Name | Mobile | Password | Role | Stores |
|---|------|--------|----------|------|--------|
| 1 | Admin (Owner) | `9999999999` | `admin123` | OWNER | Store 1 + Store 2 |
| 2 | Vikram Admin | `9876500001` | `branch123` | ADMIN | Store 1 only |
| 3 | Priya Branch Admin | `9876500002` | `branch123` | ADMIN | Store 2 only |
| 4 | Amit Billing Staff | `9876500003` | `staff123` | STAFF | Store 1 |
| 5 | Sunita Inventory Staff | `9876500004` | `staff123` | STAFF | Store 1 |
| 6 | Rohan View-Only Staff | `9876500005` | `staff123` | STAFF | Store 1 + Store 2 |

### Staff Permission Matrix

| Staff | VIEW_INV | MANAGE_INV | VIEW_CUST | MANAGE_CUST | CREATE_INV | VIEW_INV | MANAGE_INV | VIEW_REPORTS |
|-------|----------|------------|-----------|-------------|------------|----------|------------|--------------|
| Amit (Billing) | ✅ | ❌ | ✅ | ❌ | ✅ | ✅ | ✅ | ❌ |
| Sunita (Inventory) | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Rohan (View-Only) | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ |

### Pre-loaded Stores

| ID | Name | City | Organization |
|----|------|------|-------------|
| 1 | Default Store | — | Default Organization |
| 2 | MG Road Branch | New Delhi | Default Organization |

### Pre-loaded Sample Data (Store 1)

| Data | Count | Examples |
|------|-------|---------|
| Categories | 7 | Rings, Necklaces, Earrings, Bangles, Pendants, Chains, Anklets |
| Metal Types | 6 | Gold 24K/22K/18K, Silver 925/999, Platinum 950 |
| Jewelry Items | 20 | Mix of gold rings, necklaces, earrings, bangles, silver & platinum items |
| Customers | 10 | Indian customers with full addresses across Mumbai, Delhi, Jaipur, etc. |

---

## 8. Error Handling

### Error Response Format

All error responses follow this structure:

```json
{
  "message": "Descriptive error message",
  "error": "Bad Request",
  "timestamp": "2026-03-14T10:00:00.000Z"
}
```

### HTTP Status Codes

| Code | Meaning | When |
|------|---------|------|
| `200` | OK | Successful read/update |
| `201` | Created | Successful create |
| `204` | No Content | Successful delete, or empty result |
| `400` | Bad Request | Validation error, invalid input |
| `401` | Unauthorized | Missing/invalid/expired JWT |
| `403` | Forbidden | Insufficient permissions for STAFF |
| `404` | Not Found | Resource not found |
| `500` | Internal Server Error | Server-side error |

### Common Error Scenarios

```json
// 401 — Missing or expired token
{
  "error": "Unauthorized",
  "message": "Authentication required"
}

// 400 — Validation error
{
  "message": "Mobile number is required",
  "error": "Bad Request",
  "timestamp": "2026-03-14T10:00:00Z"
}

// 400 — Invalid credentials
{
  "message": "Invalid mobile or password",
  "error": "Bad Request",
  "timestamp": "2026-03-14T10:00:00Z"
}

// 403 — Permission denied (STAFF only)
{
  "message": "Access denied: requires MANAGE_BILLING permission",
  "error": "Forbidden",
  "timestamp": "2026-03-14T10:00:00Z"
}
```

---

## 9. Endpoint Count Summary

| Module | GET | POST | PUT | PATCH | DELETE | Total |
|--------|-----|------|-----|-------|--------|-------|
| Public / Health | 5 | — | — | — | — | 5 |
| Auth | — | 3 | — | — | — | 3 |
| Staff | 2 | 1 | 1 | — | 1 | 5 |
| Categories | 2 | 1 | 1 | — | 1 | 5 |
| Metal Types | 3 | 1 | 1 | — | 1 | 6 |
| Jewelry Items | 5 | 1 | 1 | — | 1 | 8 |
| Customers | 4 | 1 | 1 | — | 1 | 7 |
| Invoices | 2 | 2 | 1 | 1 | — | 6 |
| Estimates | 2 | 2 | 1 | — | — | 5 |
| Credit Notes | 2 | 1 | — | — | — | 3 |
| Ledger | 2 | 1 | — | — | — | 3 |
| Expenses | 2 | 1 | 1 | — | 1 | 5 |
| Cash Register | 2 | 1 | — | 1 | — | 4 |
| Suppliers | 2 | 1 | 1 | — | 1 | 5 |
| Rates | 3 | 1 | 1 | — | — | 5 |
| Old Gold | 2 | 1 | 1 | — | — | 4 |
| Schemes | 4 | 3 | 1 | — | — | 8 |
| Dashboard | 1 | — | — | — | — | 1 |
| Settings | 2 | — | 1 | — | — | 3 |
| Activity Logs | 1 | — | — | — | — | 1 |
| DB Admin | 5 | — | — | — | — | 5 |
| **TOTAL** | **53** | **21** | **13** | **2** | **7** | **103** |

---

> **Generated from source code on 2026-03-14.**
> **Repository:** JewelERP (Acme_Killer_Service)
> **Tech Stack:** Spring Boot 3.4.3 · Java 21 · MySQL 8.0 · JWT · ECS Fargate
