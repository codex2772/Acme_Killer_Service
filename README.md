# JewelERP — Cloud-Native Jewelry Management System

[![Build Status](https://github.com/aurajewels/jewel-erp/workflows/CI/CD/badge.svg)](https://github.com/aurajewels/jewel-erp/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![AWS ECS](https://img.shields.io/badge/Deploy-AWS%20ECS%20Fargate-blue.svg)](https://aws.amazon.com/ecs/)

> **Last Updated:** 2026-03-19
> **Total Endpoints:** 110 | **Total DB Tables:** 33+ | **Architecture:** Multi-tenant SaaS

A comprehensive cloud-native ERP backend for jewelry retailers — multi-store, multi-tenant — featuring inventory tracking, GST-compliant billing, saving schemes, S3-powered image uploads, and three client applications: **Desktop (Electron.js)**, **Admin Mobile (Flutter)**, and **Customer App (Flutter)**.

---

## Table of Contents

- [Architecture Overview](#-architecture-overview)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [Project Structure](#-project-structure)
- [API Endpoints — Admin/Staff (Desktop App)](#-api-endpoints--adminstaff-desktop-app)
- [API Endpoints — Customer (Mobile App)](#-api-endpoints--customer-mobile-app)
- [API Endpoints — Image Upload](#-api-endpoints--image-upload)
- [Authentication & Authorization](#-authentication--authorization)
- [Database Schema](#-database-schema)
- [Infrastructure (AWS)](#-infrastructure-aws)
- [CI/CD Pipelines](#-cicd-pipelines)
- [Environment Variables](#-environment-variables)
- [Deployment](#-deployment)
- [Testing](#-testing)
- [Documentation](#-documentation)
- [License](#-license)

---

## 🏗 Architecture Overview

```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Desktop App     │  │  Admin Mobile    │  │  Customer App    │
│  (Electron.js)   │  │  (Flutter)       │  │  (Flutter)       │
│  Owner/Admin/    │  │  Owner/Admin/    │  │  Browse catalog, │
│  Staff access    │  │  Staff access    │  │  wishlist, enquiry│
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                      │
         └─────────────────────┼──────────────────────┘
                               │ HTTPS
                               ▼
                      ┌────────────────┐
                      │  AWS ALB        │
                      │  (Load Balancer)│
                      └────────┬───────┘
                               │
                               ▼
                      ┌────────────────┐        ┌─────────────┐
                      │  AWS ECS Fargate│───────►│  AWS S3      │
                      │  (Spring Boot)  │        │  (Images)    │
                      └────────┬───────┘        └─────────────┘
                               │
                      ┌────────┴───────┐
                      │  AWS RDS MySQL  │
                      │  (Database)     │
                      └────────────────┘
```

**Client Applications:**

| App | Technology | Users | Purpose |
|-----|-----------|-------|---------|
| **Desktop App** | Electron.js | Owner, Admin, Staff | Full ERP — inventory, billing, accounts, schemes, reports |
| **Admin Mobile App** | Flutter | Owner, Admin, Staff | Same as Desktop — manage store on the go |
| **Customer App** | Flutter | Customers | Browse catalog, wishlist, enquiry, scheme payments, profile |

**Multi-Tenant Architecture:**
- **Organization** → owns multiple **Stores**
- **Owner** → manages the organization
- **Admin** → manages a specific store
- **Staff** → works at a store with specific permissions
- **Customer** → browses catalog, wishlists, pays scheme installments, enquires via mobile app

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.4.3 |
| **Security** | Spring Security + JWT (jjwt 0.12.6) |
| **Database** | MySQL 8.0 (AWS RDS) |
| **ORM** | Spring Data JPA / Hibernate 6 |
| **Migrations** | Flyway |
| **Image Storage** | AWS S3 (SDK v2.25.16) |
| **Container** | Docker (multi-stage, Eclipse Temurin 21) |
| **Orchestration** | AWS ECS Fargate |
| **Load Balancer** | AWS ALB |
| **Secrets** | AWS Secrets Manager |
| **IaC** | Terraform (~25 resources) |
| **CI/CD** | GitHub Actions |
| **Code Style** | Spotless + Google Java Format |

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- MySQL 8.0 (local or Docker)
- AWS CLI (for deployment)

### Local Development

```bash
# Clone the repository
git clone https://github.com/aurajewels/jewel-erp.git
cd jewel-erp

# Start MySQL via Docker
docker-compose up -d db

# Run the application
./mvnw spring-boot:run

# Or build and run
./mvnw clean package -DskipTests
java -jar target/jewel-erp-0.0.1.jar
```

### Using Docker

```bash
# Build and run everything
docker-compose up --build

# Or just the app
docker build -t jewel-erp:latest .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://host:3306/jewelerpdb \
  -e DB_USERNAME=jeweladmin \
  -e DB_PASSWORD=yourpass \
  -e JWT_SECRET=your-secret-key \
  jewel-erp:latest
```

### Verify

```bash
curl http://localhost:8080/api/hello
# → {"message":"Hello, World! JewelERP is running.","service":"jewel-erp","version":"0.0.1","status":"UP"}
```

---

## 📁 Project Structure

```
jewel-erp/
├── .github/workflows/          # CI/CD pipelines (build, deploy, terraform)
├── deploy/ecs/                 # ECS task/service definitions
├── infra/terraform/            # Terraform IaC (VPC, ECS, RDS, S3, ALB, IAM)
├── spotless/                   # License header template
├── src/
│   ├── main/
│   │   ├── java/com/aurajewels/jewel/
│   │   │   ├── config/         # Security, JWT, S3, Flyway configs
│   │   │   ├── controller/     # 22 REST controllers
│   │   │   ├── dto/            # Request/Response DTOs
│   │   │   ├── entity/         # JPA entities (33+ tables)
│   │   │   ├── exception/      # Global exception handler
│   │   │   ├── repository/     # Spring Data JPA repositories
│   │   │   ├── security/       # JWT filter, permission aspect, store context
│   │   │   └── service/        # Business logic services
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/migration/   # Flyway SQL migrations (V1–V5)
│   └── test/
├── Dockerfile                  # Multi-stage production build
├── docker-compose.yml
├── pom.xml
├── API_REFERENCE.md            # Detailed admin API docs
├── CUSTOMER_APP_API_REFERENCE.md # Customer mobile app API docs
└── README.md
```

---

## 📡 API Endpoints — Admin/Staff (Desktop App)

**Base URL:** `http://<ALB_DNS>/api`
**Auth:** All endpoints (except login/hello) require `Authorization: Bearer <JWT>`
**Store Scope:** Pass `X-Store-Id: <id>` header to scope data per store

### Auth — `/api/auth`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `POST` | `/api/auth/login` | Public | Login (mobile + password) |
| `POST` | `/api/auth/refresh-token` | Public | Refresh JWT token |
| `POST` | `/api/auth/change-password` | Authenticated | Change password |

### Staff — `/api/staff`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/staff` | MANAGE_STAFF | List all staff |
| `GET` | `/api/staff/{id}` | MANAGE_STAFF | Get staff by ID |
| `POST` | `/api/staff` | MANAGE_STAFF | Create staff/admin |
| `PUT` | `/api/staff/{id}` | MANAGE_STAFF | Update staff |
| `DELETE` | `/api/staff/{id}` | MANAGE_STAFF | Deactivate staff |

### Categories — `/api/categories`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/categories` | VIEW_INVENTORY | List all categories |
| `POST` | `/api/categories` | MANAGE_INVENTORY | Create category |
| `PUT` | `/api/categories/{id}` | MANAGE_INVENTORY | Update category |
| `DELETE` | `/api/categories/{id}` | MANAGE_INVENTORY | Delete category |

### Metal Types — `/api/metal-types`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/metal-types` | VIEW_INVENTORY | List all metal types |
| `POST` | `/api/metal-types` | MANAGE_INVENTORY | Create metal type |
| `PUT` | `/api/metal-types/{id}` | MANAGE_INVENTORY | Update metal type |
| `DELETE` | `/api/metal-types/{id}` | MANAGE_INVENTORY | Delete metal type |

### Jewelry Items (Inventory) — `/api/jewelry-items`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/jewelry-items` | VIEW_INVENTORY | List all items |
| `GET` | `/api/jewelry-items/{id}` | VIEW_INVENTORY | Get item by ID |
| `GET` | `/api/jewelry-items/sku/{sku}` | VIEW_INVENTORY | Get item by SKU |
| `GET` | `/api/jewelry-items/category/{id}` | VIEW_INVENTORY | Filter by category |
| `GET` | `/api/jewelry-items/status/{status}` | VIEW_INVENTORY | Filter by status |
| `POST` | `/api/jewelry-items` | MANAGE_INVENTORY | Create item |
| `PUT` | `/api/jewelry-items/{id}` | MANAGE_INVENTORY | Update item |
| `DELETE` | `/api/jewelry-items/{id}` | MANAGE_INVENTORY | Soft delete item |

**Item Statuses:** `IN_STOCK` | `SOLD` | `ON_APPROVAL` | `RETURNED` | `DAMAGED`

**Image Support:** Items have an `imageUrl` field. Upload via `POST /api/images/upload`, then include the URL in create/update.

### Customers — `/api/customers`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/customers` | VIEW_CUSTOMERS | List all customers |
| `GET` | `/api/customers/{id}` | VIEW_CUSTOMERS | Get customer by ID |
| `GET` | `/api/customers/search?q=` | VIEW_CUSTOMERS | Search by name/phone |
| `POST` | `/api/customers` | MANAGE_CUSTOMERS | Create customer |
| `PUT` | `/api/customers/{id}` | MANAGE_CUSTOMERS | Update customer |
| `DELETE` | `/api/customers/{id}` | MANAGE_CUSTOMERS | Deactivate customer |

### Invoices — `/api/invoices`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/invoices` | VIEW_BILLING | List all invoices |
| `GET` | `/api/invoices/{id}` | VIEW_BILLING | Get invoice with items |
| `POST` | `/api/invoices` | MANAGE_BILLING | Create invoice |
| `PATCH` | `/api/invoices/{id}/status` | MANAGE_BILLING | Update status (cancel, confirm) |

### Estimates — `/api/estimates`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/estimates` | VIEW_BILLING | List all estimates |
| `GET` | `/api/estimates/{id}` | VIEW_BILLING | Get estimate with items |
| `POST` | `/api/estimates` | MANAGE_BILLING | Create estimate |
| `POST` | `/api/estimates/{id}/convert` | MANAGE_BILLING | Convert to invoice |

### Credit Notes — `/api/credit-notes`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/credit-notes` | VIEW_BILLING | List all credit notes |
| `GET` | `/api/credit-notes/{id}` | VIEW_BILLING | Get credit note with items |
| `POST` | `/api/credit-notes` | MANAGE_BILLING | Create credit note |

### Ledger — `/api/ledger`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/ledger` | VIEW_ACCOUNTS | List ledger entries |
| `POST` | `/api/ledger` | MANAGE_ACCOUNTS | Create ledger entry |

### Expenses — `/api/expenses`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/expenses` | VIEW_ACCOUNTS | List expenses |
| `POST` | `/api/expenses` | MANAGE_ACCOUNTS | Create expense |
| `PUT` | `/api/expenses/{id}` | MANAGE_ACCOUNTS | Update expense |
| `DELETE` | `/api/expenses/{id}` | MANAGE_ACCOUNTS | Delete expense |

### Cash Register — `/api/cash-register`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/cash-register` | VIEW_ACCOUNTS | List cash register entries |
| `GET` | `/api/cash-register/today` | VIEW_ACCOUNTS | Today's summary |
| `POST` | `/api/cash-register` | MANAGE_ACCOUNTS | Create entry |

### Suppliers — `/api/suppliers`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/suppliers` | VIEW_ACCOUNTS | List suppliers |
| `GET` | `/api/suppliers/{id}` | VIEW_ACCOUNTS | Get supplier |
| `POST` | `/api/suppliers` | MANAGE_ACCOUNTS | Create supplier |
| `PUT` | `/api/suppliers/{id}` | MANAGE_ACCOUNTS | Update supplier |
| `DELETE` | `/api/suppliers/{id}` | MANAGE_ACCOUNTS | Delete supplier |

### Daily Rates — `/api/rates`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/rates` | VIEW_INVENTORY | Get today's rates |
| `GET` | `/api/rates/history` | VIEW_INVENTORY | Rate history |
| `POST` | `/api/rates` | MANAGE_INVENTORY | Set today's rates |

### Old Gold — `/api/old-gold`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/old-gold` | VIEW_ACCOUNTS | List old gold entries |
| `POST` | `/api/old-gold` | MANAGE_ACCOUNTS | Create old gold entry |

### Saving Schemes — `/api/schemes`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/schemes` | MANAGE_SCHEME | List all schemes |
| `GET` | `/api/schemes/{id}` | MANAGE_SCHEME | Get scheme details |
| `POST` | `/api/schemes` | MANAGE_SCHEME | Create scheme |
| `PUT` | `/api/schemes/{id}` | MANAGE_SCHEME | Update scheme |
| `GET` | `/api/schemes/{id}/members` | MANAGE_SCHEME | List members |
| `POST` | `/api/schemes/{id}/members` | MANAGE_SCHEME | Add member |
| `POST` | `/api/schemes/{id}/members/{mid}/payments` | MANAGE_SCHEME | Record payment |
| `GET` | `/api/schemes/{id}/members/{mid}/payments` | MANAGE_SCHEME | List payments |

### Dashboard — `/api/dashboard`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/dashboard/summary` | VIEW_REPORTS | Sales & metrics summary |

### Settings — `/api/settings`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/settings` | Authenticated | Get store settings |
| `PUT` | `/api/settings` | OWNER/ADMIN | Update settings |

### Activity Logs — `/api/activity-logs`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/activity-logs` | VIEW_REPORTS | List activity logs |

### Enquiries (Admin) — `/api/enquiries`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/enquiries` | VIEW_CUSTOMERS | List all enquiries for the store |
| `GET` | `/api/enquiries/{id}` | VIEW_CUSTOMERS | Get enquiry with customer contact details |
| `PUT` | `/api/enquiries/{id}/respond` | MANAGE_CUSTOMERS | Respond to enquiry |
| `PATCH` | `/api/enquiries/{id}/close` | MANAGE_CUSTOMERS | Close enquiry |

> Admin sees: customer name, phone, email, enquiry message, attached image, linked jewelry item. Can respond and the customer sees the reply in their app.

### Image Upload — `/api/images`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `POST` | `/api/images/upload` | MANAGE_INVENTORY | Upload image to S3 (multipart) |
| `DELETE` | `/api/images?url=` | MANAGE_INVENTORY | Delete image from S3 |

### DB Admin — `/api/db`

| Method | Endpoint | Permission | Description |
|--------|----------|------------|-------------|
| `GET` | `/api/db/tables` | OWNER | List all tables |
| `GET` | `/api/db/tables/{name}` | OWNER | Table details |

### Health / Hello

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api` | Public | Welcome message |
| `GET` | `/api/hello` | Public | Hello World + version |
| `GET` | `/actuator/health` | Public | Health check |
| `GET` | `/actuator/health/liveness` | Public | Liveness probe |
| `GET` | `/actuator/health/readiness` | Public | Readiness probe |

---

## 📱 API Endpoints — Customer (Mobile App)

**Base URL:** `http://<ALB_DNS>/api/customer-app`
**Auth:** 🔓 = Requires customer JWT | 🌐 = Public

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/customer-app/register` | 🌐 | Register new customer |
| `POST` | `/api/customer-app/login` | 🌐 | Login (phone + password) |

### Stores (Discover)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/customer-app/stores` | 🌐 | List all stores |
| `GET` | `/api/customer-app/stores/{id}` | 🌐 | Store details + categories |

### Catalog

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/customer-app/catalog/{storeId}` | 🌐 | Browse store catalog |
| `GET` | `/api/customer-app/catalog/{storeId}/items/{itemId}` | 🌐 | Item detail |

### Wishlist

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/customer-app/wishlist` | 🔓 | Get my wishlist |
| `POST` | `/api/customer-app/wishlist/{itemId}` | 🔓 | Add to wishlist |
| `DELETE` | `/api/customer-app/wishlist/{itemId}` | 🔓 | Remove from wishlist |

### Enquiry

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/customer-app/enquiry` | 🔓 | Submit enquiry (with optional image) |
| `GET` | `/api/customer-app/enquiries` | 🔓 | My enquiries |

### Image Upload (Customer)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/customer-app/images/upload` | 🔓 | Upload enquiry image to S3 |

### Profile

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/api/customer-app/profile` | 🔓 | Get my profile |
| `PUT` | `/api/customer-app/profile` | 🔓 | Update my profile |

---

## 🖼 API Endpoints — Image Upload

Images are stored in **AWS S3** (`jewel-erp-images-ap-south-1`) and served publicly via S3 URLs.

### Admin/Staff Upload (Inventory)

```
POST /api/images/upload
Authorization: Bearer <staff/admin token>
Content-Type: multipart/form-data

Form fields:
  file:   <image file>        (required, max 5MB, JPEG/PNG/WebP/GIF)
  folder: jewelry-items       (optional, default: jewelry-items)

Response: {"imageUrl": "https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com/jewelry-items/uuid.jpg"}
```

### Customer Upload (Enquiry)

```
POST /api/customer-app/images/upload
Authorization: Bearer <customer token>
Content-Type: multipart/form-data

Form fields:
  file: <image file>          (required, max 5MB, JPEG/PNG/WebP/GIF)

Response: {"imageUrl": "https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com/enquiries/uuid.jpg"}
```

### Delete Image

```
DELETE /api/images?url=<encoded_s3_url>
Authorization: Bearer <staff/admin token>

Response: {"message": "Image deleted successfully"}
```

### Flow

```
1. Upload image     → POST .../images/upload     → get imageUrl
2. Create item      → POST /api/jewelry-items     → include "imageUrl" in JSON body
3. List items       → GET  /api/jewelry-items      → each item has "imageUrl" field
4. Display in UI    → <img src={item.imageUrl} />  → served directly from S3
```

---

## 🔐 Authentication & Authorization

### JWT Token Structure

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

### Roles & Hierarchy

| Role | Scope | Description |
|------|-------|-------------|
| **OWNER** | Organization-wide | Full access to all stores. Can add admins. |
| **ADMIN** | Store-level | Full access to assigned store(s). Can add staff. |
| **STAFF** | Store-level | Access based on assigned permissions only. |
| **CUSTOMER** | Mobile app only | Browse catalog, wishlist, enquiry, profile. |

### Permissions

| Permission | Description |
|------------|-------------|
| `VIEW_INVENTORY` | View items, categories, metal types, rates |
| `MANAGE_INVENTORY` | Create/edit/delete items, categories, metal types, rates |
| `VIEW_BILLING` | View invoices, estimates, credit notes |
| `MANAGE_BILLING` | Create/edit invoices, estimates, credit notes |
| `VIEW_CUSTOMERS` | View customer list |
| `MANAGE_CUSTOMERS` | Create/edit customers |
| `VIEW_ACCOUNTS` | View ledger, expenses, cash register, suppliers |
| `MANAGE_ACCOUNTS` | Create/edit ledger, expenses, cash register, suppliers |
| `VIEW_REPORTS` | View dashboard & activity logs |
| `MANAGE_STAFF` | Create/edit staff members |
| `MANAGE_SCHEME` | Create/manage saving schemes |

> **OWNER** and **ADMIN** have all permissions implicitly. Only **STAFF** needs explicit permission assignment.

---

## 🗄 Database Schema

### Migration History

| Version | File | Description |
|---------|------|-------------|
| V1 | `V1__init_schema.sql` | Core schema — orgs, stores, users, permissions, inventory, billing, accounts, schemes (33 tables) |
| V2 | `V2__customer_app.sql` | Customer app — wishlists, enquiries |
| V3 | `V3__fix_scheme_tables.sql` | Fix missing columns in scheme_members/payments |
| V4 | `V4__add_image_url_to_jewelry_items.sql` | Add `image_url` to jewelry_items |
| V5 | `V5__add_image_url_to_enquiries.sql` | Add `image_url` to customer_enquiries |

### Key Tables

| Table | Description |
|-------|-------------|
| `organizations` | Multi-tenant orgs (jewellery businesses) |
| `stores` | Stores/branches per org |
| `users` | Owners, admins, staff |
| `permissions` | Permission definitions |
| `user_permissions` | User ↔ Permission ↔ Store mapping |
| `user_store_access` | User ↔ Store access mapping |
| `categories` | Jewelry categories (Rings, Necklaces, etc.) |
| `metal_types` | Metal types with purity & rates |
| `jewelry_items` | Inventory items with SKU, weight, charges, image |
| `customers` | Customer profiles |
| `invoices` | GST-compliant invoices |
| `invoice_items` | Line items per invoice |
| `invoice_payments` | Split payments per invoice |
| `estimates` | Price estimates |
| `estimate_items` | Line items per estimate |
| `credit_notes` | Credit/return notes |
| `credit_note_items` | Line items per credit note |
| `ledger_entries` | Accounts ledger |
| `expenses` | Business expenses |
| `cash_register` | Daily cash register |
| `suppliers` | Metal/stone suppliers |
| `supplier_metals` | Metals supplied by each supplier |
| `daily_rates` | Daily gold/silver/platinum rates |
| `old_gold_entries` | Old gold purchase records |
| `schemes` | Saving schemes |
| `scheme_members` | Members enrolled in schemes |
| `scheme_payments` | Installment payments |
| `store_settings` | Per-store configuration (GSTIN, address, etc.) |
| `activity_logs` | Audit trail |
| `customer_wishlists` | Customer wishlist items |
| `customer_enquiries` | Customer enquiries with optional image |

---

## ☁️ Infrastructure (AWS)

All infrastructure is managed via **Terraform** (`infra/terraform/`).

| Resource | Service | Purpose |
|----------|---------|---------|
| VPC + Subnets | VPC | Network isolation (public + private subnets) |
| Internet Gateway | VPC | Public internet access |
| ALB + Target Group | ELB | Load balancer → ECS tasks |
| ECS Cluster (Fargate) | ECS | Container orchestration |
| ECS Service + Task | ECS | Running the Spring Boot app |
| ECR Repository | ECR | Docker image registry |
| RDS MySQL (db.t3.micro) | RDS | Database (Free Tier) |
| S3 Bucket | S3 | Image storage (public read) |
| Secrets Manager | Secrets | DB credentials + JWT secret |
| IAM Roles + Policies | IAM | ECS task roles, S3 access |
| CloudWatch Log Group | CloudWatch | Application logs |
| Security Groups | VPC | Network firewall rules |

### Terraform Outputs

```
alb_dns_name         = jewel-erp-alb-xxx.ap-south-1.elb.amazonaws.com
ecr_repository_url   = 010741811111.dkr.ecr.ap-south-1.amazonaws.com/jewel-erp
ecs_cluster_name     = jewel-erp-cluster
ecs_service_name     = jewel-erp-service
rds_endpoint         = jewel-erp-db.xxx.ap-south-1.rds.amazonaws.com
s3_images_bucket     = jewel-erp-images-ap-south-1
s3_images_bucket_url = https://jewel-erp-images-ap-south-1.s3.ap-south-1.amazonaws.com
```

---

## 🔄 CI/CD Pipelines

### GitHub Actions Workflows

| Workflow | Trigger | Description |
|----------|---------|-------------|
| **CI** | Push to any branch | Build, test, spotless check |
| **Deploy** | Manual / push to main | Build Docker → Push ECR → Update ECS |
| **Terraform Plan** | Manual | Run `terraform plan` |
| **Terraform Apply** | Manual | Run `terraform apply` |

### Deploy Flow

```
Git Push → CI Build → Docker Build → Push to ECR → Update ECS Service → Rolling Deploy
```

---

## ⚙️ Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PORT` | Server port | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `default` |
| `DB_URL` | JDBC MySQL URL | `jdbc:mysql://localhost:3306/jewelerpdb` |
| `DB_USERNAME` | Database username | `jeweladmin` |
| `DB_PASSWORD` | Database password | `jewelpass` |
| `JWT_SECRET` | JWT signing key (min 32 chars) | dev default |
| `S3_BUCKET_NAME` | S3 bucket for images | `jewel-erp-images-ap-south-1` |
| `S3_REGION` | AWS region for S3 | `ap-south-1` |

### AWS Secrets Manager

Secret `jewel-erp/prod/db-credentials` contains:

```json
{
  "host": "jewel-erp-db.xxx.rds.amazonaws.com",
  "port": 3306,
  "dbname": "jewelerpdb",
  "username": "jeweladmin",
  "password": "xxx",
  "url": "jdbc:mysql://...",
  "jwt_secret": "xxx"
}
```

---

## 🐳 Deployment

### Manual Deploy to ECS

```bash
# 1. Login to ECR
aws ecr get-login-password --region ap-south-1 | \
  docker login --username AWS --password-stdin 010741811111.dkr.ecr.ap-south-1.amazonaws.com

# 2. Build and push image
docker build -t jewel-erp:latest .
docker tag jewel-erp:latest 010741811111.dkr.ecr.ap-south-1.amazonaws.com/jewel-erp:latest
docker push 010741811111.dkr.ecr.ap-south-1.amazonaws.com/jewel-erp:latest

# 3. Update ECS service (force new deployment)
aws ecs update-service \
  --cluster jewel-erp-cluster \
  --service jewel-erp-service \
  --force-new-deployment
```

### Health Checks

| Probe | Endpoint | Purpose |
|-------|----------|---------|
| Liveness | `/actuator/health/liveness` | Container is alive |
| Readiness | `/actuator/health/readiness` | App is ready for traffic |

---

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Format code (Spotless + Google Java Format)
./mvnw spotless:apply

# Check formatting
./mvnw spotless:check

# Build without tests
./mvnw clean package -DskipTests
```

---

## 📄 Documentation

| File | Description |
|------|-------------|
| [`API_REFERENCE.md`](API_REFERENCE.md) | Detailed admin/staff API reference (103+ endpoints, DB schema, ERD) |
| [`CUSTOMER_APP_API_REFERENCE.md`](CUSTOMER_APP_API_REFERENCE.md) | Customer mobile app API reference |
| [`ENQUIRY_IMAGE_UPLOAD.md`](ENQUIRY_IMAGE_UPLOAD.md) | Customer enquiry image upload (Flutter integration guide) |
| [`SAVING_SCHEMES.md`](SAVING_SCHEMES.md) | Saving schemes business logic |
| [`BUSINESS_PLAN.md`](BUSINESS_PLAN.md) | Business plan, pricing, GTM strategy, financial projections |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution guidelines |
| [`infra/terraform/README.md`](infra/terraform/README.md) | Terraform infrastructure guide |
| [`deploy/ecs/README.md`](deploy/ecs/README.md) | ECS deployment guide |

---

## 📜 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

```
Copyright (c) 2026 AuraJewels (Raviraj Bhosale)
```

---

## 🤝 Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.
