# JewelERP — Admin Enquiry Management (Desktop/Admin App Integration Guide)

> **Date:** March 19, 2026
> **What Changed:** New admin-side enquiry endpoints added to the backend
> **Why:** When customers submit enquiries (with optional images) via the Customer App, the admin/owner/staff need to see them, view customer contact details, respond, and close them from the Desktop or Admin Mobile app.

---

## What Was Missing Before

Previously, the only enquiry endpoints were on the **customer-app side**:

```
POST /api/customer-app/enquiry          → Customer submits enquiry
GET  /api/customer-app/enquiries        → Customer sees their own enquiries
POST /api/customer-app/images/upload    → Customer uploads reference image
```

**Problem:** The admin/owner had **NO WAY** to see or respond to these enquiries from the Desktop app. Enquiries were going into the database but invisible to the store.

---

## What Was Added (Backend Changes)

### New Admin Endpoints — `/api/enquiries`

| # | Method | Endpoint | Permission Required | Description |
|---|--------|----------|---------------------|-------------|
| 1 | `GET` | `/api/enquiries` | `VIEW_CUSTOMERS` | List all enquiries for the current store |
| 2 | `GET` | `/api/enquiries/{id}` | `VIEW_CUSTOMERS` | Get single enquiry with full details |
| 3 | `PUT` | `/api/enquiries/{id}/respond` | `MANAGE_CUSTOMERS` | Respond to a customer enquiry |
| 4 | `PATCH` | `/api/enquiries/{id}/close` | `MANAGE_CUSTOMERS` | Close an enquiry without response |

**Auth:** All endpoints require `Authorization: Bearer <JWT>` (Owner/Admin/Staff with appropriate permission).
**Store Scope:** Uses `X-Store-Id` header (same as all other admin endpoints).

---

## API Details

### 1. GET `/api/enquiries` — List All Enquiries

**Headers:**
```
Authorization: Bearer <jwt_token>
X-Store-Id: 1
```

**Response — `200 OK`:**
```json
[
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
  },
  {
    "id": 3,
    "customerId": 8,
    "customerName": "Amit Verma",
    "customerPhone": "9123456789",
    "customerEmail": null,
    "jewelryItemId": null,
    "jewelryItemName": null,
    "jewelryItemSku": null,
    "subject": "Custom necklace order",
    "message": "I want a custom 22K gold necklace, around 30 grams.",
    "imageUrl": null,
    "status": "RESPONDED",
    "adminResponse": "Yes, we can make this. Please visit the store for design discussion.",
    "respondedBy": 1,
    "respondedAt": "2026-03-19T11:00:00Z",
    "createdAt": "2026-03-18T08:00:00Z",
    "updatedAt": "2026-03-19T11:00:00Z"
  }
]
```

**Notes:**
- Ordered by `createdAt DESC` (newest first)
- `customerPhone` is always present (customers register with phone)
- `customerEmail` can be `null`
- `jewelryItemId/Name/Sku` is `null` for general enquiries (not linked to a specific item)
- `imageUrl` is `null` when customer didn't attach an image
- `status` values: `OPEN`, `RESPONDED`, `CLOSED`

---

### 2. GET `/api/enquiries/{id}` — Get Single Enquiry

**Headers:**
```
Authorization: Bearer <jwt_token>
X-Store-Id: 1
```

**Response — `200 OK`:** Same object shape as list item above.

**Error — `400 Bad Request`:**
```json
{"error": "Enquiry not found: 99"}
```

---

### 3. PUT `/api/enquiries/{id}/respond` — Respond to Enquiry

**Headers:**
```
Authorization: Bearer <jwt_token>
X-Store-Id: 1
Content-Type: application/json
```

**Request Body:**
```json
{
  "adminResponse": "Yes, size 7 is available. Visit us this weekend for a 10% discount!",
  "status": "RESPONDED"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `adminResponse` | String | ✅ | The reply message (customer will see this in their app) |
| `status` | String | ❌ | `RESPONDED` or `CLOSED` (defaults to `RESPONDED` if omitted) |

**Response — `200 OK`:** Returns the updated enquiry object with `adminResponse`, `respondedBy`, `respondedAt` populated.

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
  "message": "Hi, is this ring available in size 7?",
  "imageUrl": "https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com/enquiries/844732ce.png",
  "status": "RESPONDED",
  "adminResponse": "Yes, size 7 is available. Visit us this weekend for a 10% discount!",
  "respondedBy": 1,
  "respondedAt": "2026-03-19T12:00:00Z",
  "createdAt": "2026-03-19T10:30:00Z",
  "updatedAt": "2026-03-19T12:00:00Z"
}
```

---

### 4. PATCH `/api/enquiries/{id}/close` — Close Enquiry

**Headers:**
```
Authorization: Bearer <jwt_token>
X-Store-Id: 1
```

**No request body needed.**

**Response — `200 OK`:** Returns the enquiry with `status: "CLOSED"`.

---

## Response Object Schema

```typescript
interface AdminEnquiryResponse {
  id: number;

  // Customer contact details (so admin can call/WhatsApp them)
  customerId: number;
  customerName: string;       // "Priya Patel"
  customerPhone: string;      // "9876543210"
  customerEmail: string | null;

  // Linked jewelry item (null for general enquiries)
  jewelryItemId: number | null;
  jewelryItemName: string | null;  // "Diamond Solitaire Ring"
  jewelryItemSku: string | null;   // "RNG-G22-001"

  // Enquiry content
  subject: string;
  message: string;
  imageUrl: string | null;    // S3 URL or null

  // Status & admin response
  status: "OPEN" | "RESPONDED" | "CLOSED";
  adminResponse: string | null;
  respondedBy: number | null; // User ID of admin who responded
  respondedAt: string | null; // ISO timestamp

  // Timestamps
  createdAt: string;          // ISO timestamp
  updatedAt: string;          // ISO timestamp
}
```

---

## Enquiry Flow (End-to-End)

```
CUSTOMER APP                         BACKEND                        DESKTOP / ADMIN APP
────────────                         ───────                        ───────────────────

1. Customer opens                                               
   product detail page                                          

2. Taps "Enquire"                                               
   → picks image (optional)                                     
   → types subject + message                                    

3. POST /api/customer-app                                       
   /images/upload (if image)                                    
   ← returns imageUrl                                           

4. POST /api/customer-app                                       
   /enquiry                                                     
   {subject, message,                                           
    jewelryItemId, imageUrl}          Saves to DB               
   ← 201 Created                     (status: OPEN)            

                                                                5. Admin opens "Enquiries" tab
                                                                   → GET /api/enquiries
                                                                   ← List of all enquiries
                                                                     with customer name, phone,
                                                                     email, image, item details

                                                                6. Admin clicks an enquiry
                                                                   → sees customer phone number
                                                                   → sees attached image
                                                                   → sees linked jewelry item

                                                                7. Admin types response
                                                                   → PUT /api/enquiries/5/respond
                                                                   {adminResponse: "Yes, available!"}
                                      Updates DB                
                                      (status: RESPONDED)       

8. Customer opens                                               
   "My Enquiries" tab                                           
   → GET /api/customer-app                                      
     /enquiries                                                 
   ← sees adminResponse              
   ← status: RESPONDED               

                                                                9. (Optional) Admin closes enquiry
                                                                   → PATCH /api/enquiries/5/close
                                                                   (status: CLOSED)
```

---

## UI Suggestions for Electron.js Desktop App

### Where to Add

Add an **"Enquiries"** tab/section — suggested locations:
- **Option A:** Under the **Customers** module (since enquiries come from customers)
- **Option B:** As a standalone section in the sidebar with a notification badge

### Enquiry List View

```
┌─────────────────────────────────────────────────────────────────────┐
│  📨 Customer Enquiries                          Filter: [All ▼]    │
├──────┬───────────────┬─────────────────────┬────────┬──────────────┤
│  #   │ Customer      │ Subject             │ Status │ Date         │
├──────┼───────────────┼─────────────────────┼────────┼──────────────┤
│  5   │ Priya Patel   │ Available in size 7?│ 🟡 OPEN│ 19 Mar 10:30│
│      │ 📞 9876543210 │ 📎 Image attached   │        │              │
├──────┼───────────────┼─────────────────────┼────────┼──────────────┤
│  3   │ Amit Verma    │ Custom necklace     │ 🟢 DONE│ 18 Mar 08:00│
│      │ 📞 9123456789 │                     │        │              │
├──────┼───────────────┼─────────────────────┼────────┼──────────────┤
│  1   │ Rajesh Sharma │ Gold chain price?   │ ⚫ CLSD│ 17 Mar 15:00│
│      │ 📞 9988776655 │ 📎 Image attached   │        │              │
└──────┴───────────────┴─────────────────────┴────────┴──────────────┘
```

### Enquiry Detail / Response View

```
┌─────────────────────────────────────────────────────────────────────┐
│  Enquiry #5                                          🟡 OPEN       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  👤 Customer: Priya Patel                                          │
│  📞 Phone: 9876543210  [📱 Call] [💬 WhatsApp]                      │
│  📧 Email: priya@email.com                                         │
│                                                                     │
│  💍 Item: Diamond Solitaire Ring (RNG-G22-001)                     │
│                                                                     │
│  📝 Subject: Available in size 7?                                  │
│                                                                     │
│  💬 Message:                                                       │
│  "Hi, is this ring available in size 7? Attaching a reference."    │
│                                                                     │
│  📎 Attached Image:                                                │
│  ┌──────────────────────┐                                          │
│  │                      │                                          │
│  │   [Customer Image]   │                                          │
│  │                      │                                          │
│  └──────────────────────┘                                          │
│                                                                     │
│  ── Admin Response ───────────────────────────────────────────────  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Type your response here...                                    │  │
│  │                                                               │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  [ 📤 Send Response ]    [ ✖ Close Enquiry ]                       │
│                                                                     │
│  📅 Received: 19 Mar 2026, 10:30 AM                               │
└─────────────────────────────────────────────────────────────────────┘
```

### Status Badge Colors

| Status | Badge | Color |
|--------|-------|-------|
| `OPEN` | 🟡 Open | Yellow/Orange |
| `RESPONDED` | 🟢 Responded | Green |
| `CLOSED` | ⚫ Closed | Grey |

### Action Buttons

| Button | API Call | When |
|--------|----------|------|
| **Send Response** | `PUT /api/enquiries/{id}/respond` with `{adminResponse: "..."}` | Admin types a response |
| **Close Enquiry** | `PATCH /api/enquiries/{id}/close` | No further action needed |
| **Call Customer** | `tel:9876543210` (system phone dialer) | Admin clicks phone number |
| **WhatsApp Customer** | `https://wa.me/919876543210` (opens WhatsApp) | Admin clicks WhatsApp icon |

---

## Notification Badge (Optional but Recommended)

Show a count of `OPEN` enquiries on the sidebar:

```
📨 Enquiries (3)     ← 3 unresolved enquiries
```

**How to calculate:** Filter the `GET /api/enquiries` response where `status === "OPEN"`.

---

## Files Changed in Backend

| File | Type | Description |
|------|------|-------------|
| `controller/EnquiryController.java` | **NEW** | REST controller at `/api/enquiries` |
| `service/EnquiryService.java` | **NEW** | Business logic (list, get, respond, close) |
| `dto/enquiry/AdminEnquiryResponse.java` | **NEW** | Response DTO with customer contact details |
| `dto/enquiry/EnquiryReplyRequest.java` | **NEW** | Request DTO for admin response |
| `repository/CustomerEnquiryRepository.java` | **MODIFIED** | Added `JOIN FETCH` queries for admin-side |
| `API_REFERENCE.md` | **MODIFIED** | Added section 3.19 — Enquiries (Admin) |
| `README.md` | **MODIFIED** | Added enquiry endpoints to API table |

---

## Quick Test with cURL / Bruno

```bash
# 1. Login as owner
TOKEN=$(curl -s -X POST http://<ALB_DNS>/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"mobile":"9999999999","password":"admin123"}' | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# 2. List all enquiries
curl -s http://<ALB_DNS>/api/enquiries \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Store-Id: 1" | python3 -m json.tool

# 3. Respond to enquiry #5
curl -s -X PUT http://<ALB_DNS>/api/enquiries/5/respond \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Store-Id: 1" \
  -H "Content-Type: application/json" \
  -d '{"adminResponse": "Yes, size 7 is available. Visit us this weekend!"}' | python3 -m json.tool

# 4. Close enquiry #3
curl -s -X PATCH http://<ALB_DNS>/api/enquiries/3/close \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Store-Id: 1" | python3 -m json.tool
```
