# 🎯 Assignment: Saving Schemes Module

**Assignee:** _Your Name_  
**Project:** JewelERP Backend  
**Branch:** Create from `main` → `feature/saving-schemes`  
**Due Date:** _TBD_

---

## Overview

Build the **Saving Schemes** module for JewelERP — a jewellery management platform. Jewellery shops run monthly savings plans where customers deposit a fixed amount every month and get a bonus (usually 1 free month) at the end.

**Example:** "Gold Savings Plan" — Pay ₹5,000/month for 11 months, get the 12th month free. After 12 months, the customer can buy jewellery worth ₹60,000 (₹55,000 saved + ₹5,000 bonus).

---

## What You Need to Build

### Files to Create (11 files)

```
src/main/java/com/aurajewels/jewel/
├── controller/
│   └── SchemeController.java          ← REST controller (8 endpoints)
├── service/
│   └── SchemeService.java             ← Business logic
├── dto/
│   └── scheme/
│       ├── SchemeRequest.java         ← Create/update scheme request body
│       ├── SchemeMemberRequest.java   ← Add member request body
│       └── SchemePaymentRequest.java  ← Record payment request body
├── entity/
│   ├── Scheme.java                    ← JPA entity → schemes table
│   ├── SchemeMember.java             ← JPA entity → scheme_members table
│   └── SchemePayment.java            ← JPA entity → scheme_payments table
└── repository/
    ├── SchemeRepository.java          ← Spring Data JPA repository
    ├── SchemeMemberRepository.java    ← Spring Data JPA repository
    └── SchemePaymentRepository.java   ← Spring Data JPA repository
```

---

## API Endpoints (8 endpoints)

All endpoints require JWT authentication and `MANAGE_SCHEMES` permission.

### Schemes CRUD

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | `GET` | `/api/schemes` | List all active schemes for current store |
| 2 | `GET` | `/api/schemes/{id}` | Get a single scheme by ID |
| 3 | `POST` | `/api/schemes` | Create a new scheme |
| 4 | `PUT` | `/api/schemes/{id}` | Update a scheme |

### Members

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 5 | `GET` | `/api/schemes/{id}/members` | List all members of a scheme |
| 6 | `POST` | `/api/schemes/{id}/members` | Add a member to a scheme |

### Payments

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 7 | `POST` | `/api/schemes/{id}/members/{memberId}/payments` | Record a monthly payment |
| 8 | `GET` | `/api/schemes/{id}/members/{memberId}/payments` | Get payment history for a member |

---

## Request & Response Bodies

### `POST /api/schemes` — Create Scheme

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

**Response `201 Created`:** Returns the saved `Scheme` entity as JSON.

### `PUT /api/schemes/{id}` — Update Scheme

Same body as create. Only update non-null fields (partial update).

### `POST /api/schemes/{id}/members` — Add Member

**Request:**
```json
{
  "customerId": 1,
  "name": "Rajesh Sharma",
  "phone": "9876543210",
  "joinDate": "2025-06-15"
}
```
- `customerId` is optional — if provided, link the member to an existing customer
- `joinDate` defaults to today if not provided

**Response `201 Created`:** Returns the saved `SchemeMember` entity.

### `POST /api/schemes/{id}/members/{memberId}/payments` — Record Payment

**Request:**
```json
{
  "month": 1,
  "amount": 5000.00,
  "date": "2025-07-01"
}
```
- `date` defaults to today if not provided

**Response `201 Created`:** Returns the saved `SchemePayment` entity.

---

## Database Tables (Already exist — created by V6 migration)

### `schemes`

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK |
| store_id | BIGINT | NO | | FK → stores |
| name | VARCHAR(200) | NO | | Scheme name |
| duration_months | INT | NO | | e.g., 11 |
| monthly_amount | DECIMAL(14,2) | NO | | |
| status | ENUM('ACTIVE','CLOSED','PAUSED') | YES | 'ACTIVE' | |
| start_date | DATE | NO | | |
| end_date | DATE | NO | | |
| bonus_month | BOOLEAN | YES | FALSE | Free month benefit |
| description | TEXT | YES | | |
| active | BOOLEAN | YES | TRUE | Soft delete flag |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | YES | ON UPDATE | |

### `scheme_members`

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK |
| scheme_id | BIGINT | NO | | FK → schemes |
| customer_id | BIGINT | YES | | FK → customers (optional) |
| name | VARCHAR(200) | NO | | Member name |
| phone | VARCHAR(15) | YES | | |
| join_date | DATE | NO | | |
| status | ENUM('ACTIVE','COMPLETED','DROPPED') | YES | 'ACTIVE' | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | YES | ON UPDATE | |

### `scheme_payments`

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK |
| scheme_member_id | BIGINT | NO | | FK → scheme_members |
| month_number | INT | NO | | 1, 2, 3... |
| amount | DECIMAL(14,2) | NO | | |
| payment_date | DATE | NO | | |
| status | ENUM('PAID','PENDING','LATE') | YES | 'PAID' | |
| created_at | TIMESTAMP | YES | CURRENT_TIMESTAMP | |

### Relationships
```
schemes (1) ──→ (∞) scheme_members (1) ──→ (∞) scheme_payments
                         │
                         └──→ customers (optional FK)
```

---

## Step-by-Step Implementation Guide

### Step 1: Create Entities (3 files)

Create JPA entity classes that map to the database tables.

**Reference:** Look at `entity/Supplier.java` or `entity/Expense.java` for patterns.

**Key patterns:**
```java
@Entity
@Table(name = "schemes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Scheme {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // ... other fields

    // For enums stored in DB:
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SchemeStatus status;

    public enum SchemeStatus { ACTIVE, CLOSED, PAUSED }

    // For timestamps:
    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
```

**Important relationships:**
- `Scheme` has a `@ManyToOne` to `Store`
- `SchemeMember` has a `@ManyToOne` to `Scheme` and an optional `@ManyToOne` to `Customer`
- `SchemePayment` has a `@ManyToOne` to `SchemeMember`

### Step 2: Create Repositories (3 files)

Spring Data JPA interfaces that provide database queries.

**Reference:** Look at `repository/SupplierRepository.java` for patterns.

```java
@Repository
public interface SchemeRepository extends JpaRepository<Scheme, Long> {
    List<Scheme> findByStoreIdAndActiveTrueOrderByCreatedAtDesc(Long storeId);
    Optional<Scheme> findByIdAndStoreId(Long id, Long storeId);
}
```

**Repository methods you'll need:**

| Repository | Method |
|------------|--------|
| `SchemeRepository` | `findByStoreIdAndActiveTrueOrderByCreatedAtDesc(Long storeId)` |
| `SchemeRepository` | `findByIdAndStoreId(Long id, Long storeId)` |
| `SchemeMemberRepository` | `findBySchemeId(Long schemeId)` |
| `SchemeMemberRepository` | `findByIdAndSchemeId(Long id, Long schemeId)` |
| `SchemePaymentRepository` | `findBySchemeMemberIdOrderByMonthNumberAsc(Long memberId)` |

### Step 3: Create DTOs (3 files)

Request body classes. Simple POJOs with Lombok `@Data`.

**Reference:** Look at `dto/supplier/SupplierRequest.java`.

```java
@Data
public class SchemeRequest {
    private String name;
    private Integer durationMonths;
    private BigDecimal monthlyAmount;
    private String status;           // "ACTIVE", "CLOSED", "PAUSED"
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean bonusMonth;
    private String description;
}
```

### Step 4: Create Service (1 file)

Business logic — the main implementation work.

**Reference:** Look at `service/SupplierService.java` or `service/ExpenseService.java`.

**Key patterns to follow:**
```java
@Service
@RequiredArgsConstructor
public class SchemeService {
    private final SchemeRepository schemeRepository;
    private final SchemeMemberRepository schemeMemberRepository;
    private final SchemePaymentRepository schemePaymentRepository;
    private final CustomerRepository customerRepository;    // already exists
    private final StoreRepository storeRepository;          // already exists
    private final ActivityLogService activityLogService;     // already exists

    // Always get the current store:
    Long storeId = StoreContext.getCurrentStoreId();

    // Always get the current user:
    Long userId = StoreContext.getCurrentUserId();

    // Log actions:
    activityLogService.log("Created Scheme", "Scheme: Gold Savings Plan", "Schemes", "SCHEME", scheme.getId());

    // Throw for not-found:
    throw new IllegalArgumentException("Scheme not found");
}
```

**Important rules:**
1. Use `@Transactional` for writes, `@Transactional(readOnly = true)` for reads
2. Always filter by `storeId` for multi-tenant isolation
3. When getting a scheme by ID, always verify it belongs to the current store
4. Log all create actions using `activityLogService`

### Step 5: Create Controller (1 file)

REST endpoints that delegate to the service.

**Reference:** Look at `controller/SupplierController.java`.

```java
@RestController
@RequestMapping("/api/schemes")
@RequiredArgsConstructor
public class SchemeController {
    private final SchemeService schemeService;

    @GetMapping
    @RequiresPermission("MANAGE_SCHEMES")
    public ResponseEntity<List<Scheme>> list() {
        return ResponseEntity.ok(schemeService.listSchemes());
    }

    @PostMapping
    @RequiresPermission("MANAGE_SCHEMES")
    public ResponseEntity<Scheme> create(@RequestBody SchemeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(schemeService.createScheme(request));
    }
    // ... etc
}
```

**Important:**
- Add `@RequiresPermission("MANAGE_SCHEMES")` to every endpoint
- Use `HttpStatus.CREATED` (201) for POST responses
- Use `HttpStatus.OK` (200) for GET/PUT responses

---

## Code Standards

1. **License header** — every `.java` file must start with the MIT license header. Run `./mvnw spotless:apply` to auto-add it.
2. **Author tag** — add `/** @author YourName */` Javadoc before the class declaration.
3. **Formatting** — run `./mvnw spotless:apply` before committing. It enforces Google Java Format (AOSP style).
4. **Package structure:**
   - Controller → `com.aurajewels.jewel.controller`
   - Service → `com.aurajewels.jewel.service`
   - DTO → `com.aurajewels.jewel.dto.scheme`
   - Entity → `com.aurajewels.jewel.entity`
   - Repository → `com.aurajewels.jewel.repository`

---

## How to Test

### 1. Build
```bash
./mvnw clean compile
```

### 2. Run locally (needs MySQL)
```bash
docker-compose up -d    # starts MySQL
./mvnw spring-boot:run
```

### 3. Login to get JWT
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"mobile":"9999999999","password":"admin123"}'
```
Copy the `token` from response.

### 4. Test your endpoints
```bash
# Create a scheme
curl -X POST http://localhost:8080/api/schemes \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Gold Savings Plan",
    "durationMonths": 11,
    "monthlyAmount": 5000.00,
    "startDate": "2025-06-01",
    "endDate": "2026-05-01",
    "bonusMonth": true,
    "description": "Pay 5000/month for 11 months, get 12th month free."
  }'

# List schemes
curl http://localhost:8080/api/schemes \
  -H "Authorization: Bearer <TOKEN>"

# Add a member
curl -X POST http://localhost:8080/api/schemes/1/members \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Rajesh Sharma","phone":"9876543210","joinDate":"2025-06-15"}'

# Record payment
curl -X POST http://localhost:8080/api/schemes/1/members/1/payments \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"month":1,"amount":5000.00,"date":"2025-07-01"}'

# Get payment history
curl http://localhost:8080/api/schemes/1/members/1/payments \
  -H "Authorization: Bearer <TOKEN>"
```

### 5. Format code
```bash
./mvnw spotless:apply
```

---

## Checklist

- [ ] `Scheme.java` entity with all fields and enums
- [ ] `SchemeMember.java` entity with FK to Scheme and optional FK to Customer
- [ ] `SchemePayment.java` entity with FK to SchemeMember
- [ ] `SchemeRepository.java` with required query methods
- [ ] `SchemeMemberRepository.java` with required query methods
- [ ] `SchemePaymentRepository.java` with required query methods
- [ ] `SchemeRequest.java` DTO
- [ ] `SchemeMemberRequest.java` DTO
- [ ] `SchemePaymentRequest.java` DTO
- [ ] `SchemeService.java` with all 8 methods implemented
- [ ] `SchemeController.java` with all 8 endpoints mapped
- [ ] All endpoints use `@RequiresPermission("MANAGE_SCHEMES")`
- [ ] Activity logging on create actions
- [ ] Multi-tenant: all queries scoped by `storeId`
- [ ] `./mvnw clean compile` passes
- [ ] `./mvnw spotless:apply` passes
- [ ] All 8 endpoints tested via curl
- [ ] `/** @author YourName */` on all classes
- [ ] PR raised from `feature/saving-schemes` → `main`

---

## Reference Files to Study

| Pattern | Look at this file |
|---------|-------------------|
| Entity with enums + Store FK | `entity/Supplier.java`, `entity/Expense.java` |
| Entity with child relationship | `entity/OldGoldPurchase.java` (has purity tests + melting records) |
| Repository with custom queries | `repository/SupplierRepository.java` |
| Service with full CRUD + activity log | `service/SupplierService.java`, `service/ExpenseService.java` |
| Service with parent-child operations | `service/OldGoldService.java` |
| Controller with permissions | `controller/SupplierController.java` |
| DTO request class | `dto/supplier/SupplierRequest.java` |

---

> **Questions?** Reach out to the team lead. Good luck! 🚀
