# JewelERP — Customer Mobile App API Reference

> **Base URL:** `http://jewel-erp-alb-2124014483.ap-south-1.elb.amazonaws.com`
>
> **Auth:** Endpoints marked 🔓 require a `Bearer <token>` header (customer JWT).
> Endpoints marked 🌐 are public.

---

## 1. Authentication

### 1.1 Register 🌐

Creates a new customer account and returns JWT tokens.

```
POST /api/customer-app/register
```

**Request Body:**
```json
{
  "firstName": "Priya",
  "lastName": "Sharma",
  "phone": "9876543210",
  "email": "priya@example.com",
  "password": "mypassword123",
  "storeId": 1
}
```

| Field       | Type   | Required | Description                        |
|-------------|--------|----------|------------------------------------|
| firstName   | String | ✅       | Customer's first name              |
| lastName    | String | ❌       | Customer's last name               |
| phone       | String | ✅       | Mobile number (unique per store)   |
| email       | String | ❌       | Email address                      |
| password    | String | ✅       | Min 6 characters                   |
| storeId     | Long   | ✅       | Store to register with             |

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "customerId": 11,
  "name": "Priya",
  "phone": "9876543210",
  "email": "priya@example.com",
  "storeId": 1,
  "storeName": "Default Store"
}
```

**Errors:**
| Status | Message |
|--------|---------|
| 400    | Phone number is required |
| 400    | Password must be at least 6 characters |
| 400    | First name is required |
| 400    | Store ID is required |
| 400    | An account with this phone number already exists for this store |
| 400    | Store not found |

---

### 1.2 Login 🌐

Authenticates an existing customer and returns JWT tokens.

```
POST /api/customer-app/login
```

**Request Body:**
```json
{
  "phone": "9876543210",
  "password": "mypassword123",
  "storeId": 1
}
```

| Field    | Type   | Required | Description                                   |
|----------|--------|----------|-----------------------------------------------|
| phone    | String | ✅       | Registered mobile number                      |
| password | String | ✅       | Account password                              |
| storeId  | Long   | ❌       | Optional — defaults to customer's home store  |

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "customerId": 11,
  "name": "Priya",
  "phone": "9876543210",
  "email": "priya@example.com",
  "storeId": 1,
  "storeName": "Default Store"
}
```

**Errors:**
| Status | Message |
|--------|---------|
| 400    | Phone and password are required |
| 400    | Invalid phone number or password |
| 400    | Account not set up for mobile login. Please register first. |

---

## 2. Stores (Discover Stores)

### 2.1 List All Stores 🌐

Returns all active stores across all organizations. This is the first screen in the customer app — the customer picks a store to browse.

```
GET /api/customer-app/stores
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "AuraJewels - Pune",
    "organizationName": "AuraJewels Pvt Ltd",
    "address": "MG Road, Camp",
    "city": "Pune",
    "state": "Maharashtra",
    "pincode": "411001",
    "phone": "9876543210"
  },
  {
    "id": 2,
    "name": "AuraJewels - Mumbai",
    "organizationName": "AuraJewels Pvt Ltd",
    "address": "Linking Road, Bandra",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400050",
    "phone": "9876543211"
  },
  {
    "id": 3,
    "name": "Sharma Jewellers",
    "organizationName": "Sharma & Sons",
    "address": "Karol Bagh",
    "city": "New Delhi",
    "state": "Delhi",
    "pincode": "110005",
    "phone": "9812345678"
  }
]
```

| Field            | Type   | Description                          |
|------------------|--------|--------------------------------------|
| id               | Long   | Store ID (use for catalog/register)  |
| name             | String | Store name                           |
| organizationName | String | Parent organization name             |
| address          | String | Street address                       |
| city             | String | City                                 |
| state            | String | State                                |
| pincode          | String | PIN code                             |
| phone            | String | Store contact number                 |

---

### 2.2 Get Store Details 🌐

Returns detailed information about a specific store including categories and catalog summary.

```
GET /api/customer-app/stores/{storeId}
```

**Path Parameters:**
| Param   | Type | Description |
|---------|------|-------------|
| storeId | Long | Store ID    |

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "AuraJewels - Pune",
  "organizationName": "AuraJewels Pvt Ltd",
  "address": "MG Road, Camp",
  "city": "Pune",
  "state": "Maharashtra",
  "pincode": "411001",
  "phone": "9876543210",
  "gstin": "27AABCU9603R1ZM",
  "categories": [
    {
      "id": 1,
      "name": "Rings",
      "description": "All types of rings",
      "itemCount": 5
    },
    {
      "id": 2,
      "name": "Necklaces",
      "description": "Chains and necklaces",
      "itemCount": 8
    },
    {
      "id": 3,
      "name": "Earrings",
      "description": "Studs, drops, and hoops",
      "itemCount": 3
    }
  ],
  "totalItems": 16
}
```

| Field            | Type           | Description                         |
|------------------|----------------|-------------------------------------|
| id               | Long           | Store ID                            |
| name             | String         | Store name                          |
| organizationName | String         | Parent organization name            |
| address          | String         | Street address                      |
| city             | String         | City                                |
| state            | String         | State                               |
| pincode          | String         | PIN code                            |
| phone            | String         | Store contact number                |
| gstin            | String         | GSTIN                               |
| categories       | Array          | List of categories with item counts |
| totalItems       | Integer        | Total in-stock items in the store   |

**Errors:**
| Status | Message |
|--------|---------|
| 400    | Store not found |

---

## 3. Catalog (Browse Products)

### 3.1 Get Store Catalog 🌐

Returns all in-stock jewelry items for a given store. If the customer is logged in (sends JWT), each item includes a `wishlisted` flag.

```
GET /api/customer-app/catalog/{storeId}
```

**Path Parameters:**
| Param   | Type | Description |
|---------|------|-------------|
| storeId | Long | Store ID    |

**Headers (optional):**
```
Authorization: Bearer <customer_jwt_token>
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "sku": "RNG-G22-001",
    "name": "Classic Gold Band",
    "description": "Simple 22K gold band ring",
    "categoryName": "Rings",
    "metalName": "Gold",
    "purity": "22K",
    "grossWeight": 5.200,
    "netWeight": 5.000,
    "makingCharges": 1500.00,
    "stoneCharges": 0.00,
    "metalRate": 6900.00,
    "hsnCode": "7113",
    "status": "IN_STOCK",
    "wishlisted": false,
    "createdAt": "2026-03-15T10:30:00Z"
  },
  {
    "id": 2,
    "sku": "RNG-G22-002",
    "name": "Diamond Solitaire Ring",
    "description": "22K gold ring with single diamond",
    "categoryName": "Rings",
    "metalName": "Gold",
    "purity": "22K",
    "grossWeight": 4.800,
    "netWeight": 4.200,
    "makingCharges": 2500.00,
    "stoneCharges": 15000.00,
    "metalRate": 6900.00,
    "hsnCode": "7113",
    "status": "IN_STOCK",
    "wishlisted": true,
    "createdAt": "2026-03-15T10:30:00Z"
  }
]
```

---

### 3.2 Get Catalog Item Detail 🌐

Returns a single jewelry item's full details.

```
GET /api/customer-app/catalog/{storeId}/items/{itemId}
```

**Path Parameters:**
| Param   | Type | Description      |
|---------|------|------------------|
| storeId | Long | Store ID         |
| itemId  | Long | Jewelry item ID  |

**Headers (optional):**
```
Authorization: Bearer <customer_jwt_token>
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "sku": "RNG-G22-001",
  "name": "Classic Gold Band",
  "description": "Simple 22K gold band ring",
  "categoryName": "Rings",
  "metalName": "Gold",
  "purity": "22K",
  "grossWeight": 5.200,
  "netWeight": 5.000,
  "makingCharges": 1500.00,
  "stoneCharges": 0.00,
  "metalRate": 6900.00,
  "hsnCode": "7113",
  "status": "IN_STOCK",
  "wishlisted": false,
  "createdAt": "2026-03-15T10:30:00Z"
}
```

**Errors:**
| Status | Message |
|--------|---------|
| 400    | Item not found |

---

## 4. Wishlist

### 4.1 Get Wishlist 🔓

Returns all items in the customer's wishlist.

```
GET /api/customer-app/wishlist
```

**Headers:**
```
Authorization: Bearer <customer_jwt_token>
```

**Response:** `200 OK`
```json
[
  {
    "id": 2,
    "sku": "RNG-G22-002",
    "name": "Diamond Solitaire Ring",
    "description": "22K gold ring with single diamond",
    "categoryName": "Rings",
    "metalName": "Gold",
    "purity": "22K",
    "grossWeight": 4.800,
    "netWeight": 4.200,
    "makingCharges": 2500.00,
    "stoneCharges": 15000.00,
    "metalRate": 6900.00,
    "hsnCode": "7113",
    "status": "IN_STOCK",
    "wishlisted": true,
    "createdAt": "2026-03-15T10:30:00Z"
  }
]
```

---

### 4.2 Add to Wishlist 🔓

Adds a jewelry item to the customer's wishlist.

```
POST /api/customer-app/wishlist/{jewelryItemId}
```

**Path Parameters:**
| Param          | Type | Description      |
|----------------|------|------------------|
| jewelryItemId  | Long | Jewelry item ID  |

**Headers:**
```
Authorization: Bearer <customer_jwt_token>
```

**Response:** `201 Created`
```json
{
  "message": "Item added to wishlist"
}
```

**Errors:**
| Status | Message |
|--------|---------|
| 400    | Item already in wishlist |
| 400    | Item not found |

---

### 4.3 Remove from Wishlist 🔓

Removes a jewelry item from the customer's wishlist.

```
DELETE /api/customer-app/wishlist/{jewelryItemId}
```

**Path Parameters:**
| Param          | Type | Description      |
|----------------|------|------------------|
| jewelryItemId  | Long | Jewelry item ID  |

**Headers:**
```
Authorization: Bearer <customer_jwt_token>
```

**Response:** `204 No Content`

**Errors:**
| Status | Message |
|--------|---------|
| 400    | Item not in wishlist |

---

## 5. Enquiry

### 5.1 Create Enquiry 🔓

Sends an enquiry about a product or general question to the store.

```
POST /api/customer-app/enquiry
```

**Headers:**
```
Authorization: Bearer <customer_jwt_token>
```

**Request Body:**
```json
{
  "jewelryItemId": 2,
  "subject": "Availability in size 7",
  "message": "Hi, is the Diamond Solitaire Ring available in size 7? I would like to visit the store this weekend."
}
```

| Field          | Type   | Required | Description                                |
|----------------|--------|----------|--------------------------------------------|
| jewelryItemId  | Long   | ❌       | Jewelry item (null for general enquiry)    |
| subject        | String | ✅       | Enquiry subject line                       |
| message        | String | ✅       | Enquiry message body                       |

**Response:** `201 Created`
```json
{
  "id": 1,
  "jewelryItemId": 2,
  "jewelryItemName": "Diamond Solitaire Ring",
  "subject": "Availability in size 7",
  "message": "Hi, is the Diamond Solitaire Ring available in size 7? I would like to visit the store this weekend.",
  "status": "OPEN",
  "adminResponse": null,
  "createdAt": "2026-03-16T10:15:00Z",
  "respondedAt": null
}
```

**Errors:**
| Status | Message |
|--------|---------|
| 400    | Subject is required |
| 400    | Message is required |
| 400    | Item not found |

---

### 5.2 Get My Enquiries 🔓

Returns all enquiries submitted by the customer, newest first.

```
GET /api/customer-app/enquiries
```

**Headers:**
```
Authorization: Bearer <customer_jwt_token>
```

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "jewelryItemId": 2,
    "jewelryItemName": "Diamond Solitaire Ring",
    "subject": "Availability in size 7",
    "message": "Hi, is the Diamond Solitaire Ring available in size 7?",
    "status": "RESPONDED",
    "adminResponse": "Yes, size 7 is available. Please visit us anytime between 10 AM - 8 PM.",
    "createdAt": "2026-03-16T10:15:00Z",
    "respondedAt": "2026-03-16T11:30:00Z"
  },
  {
    "id": 2,
    "jewelryItemId": null,
    "jewelryItemName": null,
    "subject": "Store timings on Sunday",
    "message": "Are you open on Sundays?",
    "status": "OPEN",
    "adminResponse": null,
    "createdAt": "2026-03-16T12:00:00Z",
    "respondedAt": null
  }
]
```

**Enquiry Statuses:**
| Status      | Description                          |
|-------------|--------------------------------------|
| `OPEN`      | Enquiry submitted, awaiting response |
| `RESPONDED` | Store admin has responded            |
| `CLOSED`    | Enquiry resolved and closed          |

---

## 6. Profile

### 6.1 Get Profile 🔓

Returns the customer's profile information.

```
GET /api/customer-app/profile
```

**Headers:**
```
Authorization: Bearer <customer_jwt_token>
```

**Response:** `200 OK`
```json
{
  "id": 11,
  "firstName": "Priya",
  "lastName": "Sharma",
  "phone": "9876543210",
  "email": "priya@example.com",
  "addressLine1": "12 MG Road",
  "addressLine2": null,
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400001"
}
```

---

### 6.2 Update Profile 🔓

Updates the customer's profile. Only non-null fields are updated (partial update).

```
PUT /api/customer-app/profile
```

**Headers:**
```
Authorization: Bearer <customer_jwt_token>
```

**Request Body (all fields optional):**
```json
{
  "firstName": "Priya",
  "lastName": "Sharma Patel",
  "email": "priya.new@example.com",
  "addressLine1": "45 Ring Road",
  "addressLine2": "Near Central Mall",
  "city": "Ahmedabad",
  "state": "Gujarat",
  "pincode": "380001"
}
```

| Field        | Type   | Required | Description        |
|--------------|--------|----------|--------------------|
| firstName    | String | ❌       | First name         |
| lastName     | String | ❌       | Last name          |
| email        | String | ❌       | Email              |
| addressLine1 | String | ❌       | Address line 1     |
| addressLine2 | String | ❌       | Address line 2     |
| city         | String | ❌       | City               |
| state        | String | ❌       | State              |
| pincode      | String | ❌       | Pincode            |

> **Note:** `phone` cannot be changed via this endpoint (it's the login identifier).

**Response:** `200 OK`
```json
{
  "id": 11,
  "firstName": "Priya",
  "lastName": "Sharma Patel",
  "phone": "9876543210",
  "email": "priya.new@example.com",
  "addressLine1": "45 Ring Road",
  "addressLine2": "Near Central Mall",
  "city": "Ahmedabad",
  "state": "Gujarat",
  "pincode": "380001"
}
```

---

## JWT Token Structure

Customer JWT tokens have the following claims:

```json
{
  "sub": "11",
  "storeId": 1,
  "role": "CUSTOMER",
  "name": "Priya",
  "permissions": [],
  "iat": 1773878400,
  "exp": 1773964800
}
```

| Claim       | Description                          |
|-------------|--------------------------------------|
| sub         | Customer ID (string)                 |
| storeId     | Store the customer is browsing       |
| role        | Always `CUSTOMER`                    |
| name        | Customer's first name                |
| permissions | Always empty array for customers     |
| iat         | Issued at (epoch seconds)            |
| exp         | Expiry (24 hours from issue)         |

---

## Error Response Format

All errors follow this structure:

```json
{
  "error": "Bad Request",
  "message": "Phone number is required",
  "timestamp": "2026-03-16T10:15:00Z"
}
```

| Status | Error Type           | When                                        |
|--------|----------------------|---------------------------------------------|
| 400    | Bad Request          | Validation errors, invalid input             |
| 401    | Unauthorized         | Missing or expired JWT token                 |
| 403    | Forbidden            | Staff/owner token used on customer endpoint  |
| 500    | Internal Server Error| Unexpected server error                      |

---

## Quick Start — cURL Examples

### List All Stores
```bash
curl http://<ALB_DNS>/api/customer-app/stores
```

### Get Store Details
```bash
curl http://<ALB_DNS>/api/customer-app/stores/1
```

### Register
```bash
curl -X POST http://<ALB_DNS>/api/customer-app/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Priya",
    "lastName": "Sharma",
    "phone": "9876543210",
    "password": "mypassword123",
    "storeId": 1
  }'
```

### Login
```bash
curl -X POST http://<ALB_DNS>/api/customer-app/login \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "9876543210",
    "password": "mypassword123"
  }'
```

### Browse Catalog
```bash
curl http://<ALB_DNS>/api/customer-app/catalog/1
```

### Add to Wishlist
```bash
curl -X POST http://<ALB_DNS>/api/customer-app/wishlist/2 \
  -H "Authorization: Bearer <TOKEN>"
```

### Send Enquiry
```bash
curl -X POST http://<ALB_DNS>/api/customer-app/enquiry \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "jewelryItemId": 2,
    "subject": "Size availability",
    "message": "Is this ring available in size 7?"
  }'
```

### View Profile
```bash
curl http://<ALB_DNS>/api/customer-app/profile \
  -H "Authorization: Bearer <TOKEN>"
```
