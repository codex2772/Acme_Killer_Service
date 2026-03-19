# JewelERP — Business Plan & Product Strategy

> **Version:** 1.0 | **Date:** March 19, 2026
> **Author:** Raviraj Bhosale | **Company:** AuraJewels
> **Target Market:** Indian Jewellery Retailers (Tier-2/Tier-3 Cities)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Solution — JewelERP](#3-solution--jewelerp)
4. [Product Overview](#4-product-overview)
5. [Target Market](#5-target-market)
6. [Competitive Analysis](#6-competitive-analysis)
7. [Revenue Model & Pricing](#7-revenue-model--pricing)
8. [Cost Analysis](#8-cost-analysis)
9. [Go-to-Market Strategy](#9-go-to-market-strategy)
10. [Technology Stack & Architecture](#10-technology-stack--architecture)
11. [Product Roadmap](#11-product-roadmap)
12. [Team & Roles](#12-team--roles)
13. [Financial Projections](#13-financial-projections)
14. [Risks & Mitigation](#14-risks--mitigation)
15. [Key Metrics (KPIs)](#15-key-metrics-kpis)
16. [Legal & Compliance](#16-legal--compliance)
17. [Exit Strategy / Long-Term Vision](#17-exit-strategy--long-term-vision)

---

## 1. Executive Summary

**JewelERP** is a cloud-native, multi-tenant SaaS platform designed specifically for Indian jewellery retailers. It provides a complete business management solution — inventory tracking, GST-compliant billing, saving schemes, customer engagement, and real-time analytics — through three purpose-built client applications:

| Application | Technology | Users |
|-------------|-----------|-------|
| **Desktop App** | Electron.js | Owner, Admin, Staff (in-store billing counter) |
| **Admin Mobile App** | Flutter | Owner, Admin, Staff (manage on-the-go) |
| **Customer App** | Flutter | End customers (browse, wishlist, enquire, pay scheme installments) |

**Key Differentiators:**
- Cloud-native (access from anywhere, no local server needed)
- Multi-store support out of the box
- Customer-facing mobile app (no competitor offers this)
- Modern tech stack (Spring Boot 3, Java 21, AWS)
- Pay-as-you-go SaaS model (vs. ₹50K+ one-time license)

**Target Market:** 500,000+ jewellery retailers in India, starting with Satara, Maharashtra and expanding to Tier-2/Tier-3 cities.

**Revenue Target:** ₹50 Lakh ARR within 18 months.

---

## 2. Problem Statement

### What Jewellery Store Owners Face Today

| Problem | Impact |
|---------|--------|
| **Manual bookkeeping** | Errors in weight calculations, billing mistakes, GST non-compliance |
| **No inventory tracking** | Don't know what's in stock, lost items, theft goes unnoticed |
| **Outdated software** | On-premise, Windows-only, ugly UI, no mobile access |
| **No customer engagement** | Customers forget about the store, no loyalty programs |
| **Saving schemes on paper** | Manual tracking of scheme payments, disputes with customers |
| **Multi-store chaos** | Each branch has separate systems, no consolidated view |
| **High software cost** | ₹30K-₹60K upfront for outdated software + annual maintenance |
| **No digital presence** | Customers can't browse products online |

### Real Conversations (From Satara Jewellers)

> *"We use a register book for saving schemes. Sometimes we argue with customers about how much they paid."*
> — Small jeweller, Satara

> *"The software we bought 5 years ago doesn't work on new Windows. The vendor wants ₹20K to upgrade."*
> — Medium jeweller, Pune

> *"My competitor started showing products on Instagram. I don't know how to do that."*
> — Traditional jeweller, Karad

---

## 3. Solution — JewelERP

### How JewelERP Solves Each Problem

| Problem | JewelERP Solution |
|---------|-------------------|
| Manual bookkeeping | Automated GST-compliant billing with invoice/estimate/credit note |
| No inventory tracking | Barcode/SKU-based inventory with image upload, weight tracking |
| Outdated software | Cloud-native, works on any device, always up-to-date |
| No customer engagement | Customer mobile app with catalog, wishlist, enquiry |
| Saving schemes on paper | Digital scheme management with payment tracking |
| Multi-store chaos | Single dashboard for all stores, role-based access |
| High software cost | ₹799/month — no upfront cost, cancel anytime |
| No digital presence | Customer app = instant digital catalog |

### The "Aha!" Moments for Store Owners

1. **"I can see all my stores' sales on my phone while having tea at home"** — Multi-store dashboard on Admin Mobile App
2. **"My customer just browsed my products and sent an enquiry from the app"** — Customer App catalog + enquiry
3. **"Scheme payments are recorded digitally, no more disputes"** — Saving scheme management
4. **"I created an invoice with GST in 30 seconds"** — Fast billing with auto-GST

---

## 4. Product Overview

### 4.1 Desktop App (Electron.js)

**Primary use:** In-store billing counter, daily operations

**Modules:**

| Module | Features |
|--------|----------|
| **Dashboard** | Today's sales, monthly revenue, top items, alerts |
| **Inventory** | Add/edit items, SKU/barcode, weight, image upload, stock status |
| **Billing** | Invoice, estimate, credit note — GST auto-calculation, split payment |
| **Customers** | Customer database, purchase history, search |
| **Accounts** | Ledger, expenses, cash register, suppliers, old gold |
| **Rates** | Daily gold/silver/platinum rates, rate history |
| **Schemes** | Create schemes, add members, record payments |
| **Reports** | Sales reports, inventory reports, GST reports |
| **Staff** | Add/manage staff with role-based permissions |
| **Settings** | Store profile, GSTIN, invoice prefix, business details |

### 4.2 Admin Mobile App (Flutter)

**Primary use:** Owner/admin managing store on-the-go

**Same features as Desktop** — optimized for mobile UI:
- View dashboard & sales
- Check inventory
- Manage customers
- View invoices
- Monitor staff
- Approve estimates
- Track scheme payments

### 4.3 Customer App (Flutter)

**Primary use:** Customer engagement & loyalty

| Feature | Description |
|---------|-------------|
| **Store Discovery** | Browse all registered jewellery stores |
| **Catalog** | View store's jewelry items with images, weight, metal type |
| **Wishlist** | Save favorite items |
| **Enquiry** | Ask about product availability, size, price (with image attachment) |
| **Scheme Payments** | View scheme membership, pay installments (upcoming) |
| **Profile** | Manage personal details |
| **Notifications** | Birthday wishes, offers, new arrivals (upcoming) |

### 4.4 Backend (Spring Boot)

| Component | Details |
|-----------|---------|
| **API Endpoints** | 110+ REST APIs |
| **Database Tables** | 33+ tables |
| **Authentication** | JWT-based (access + refresh tokens) |
| **Authorization** | Role-based (Owner → Admin → Staff → Customer) + Permission-based |
| **Multi-tenancy** | Organization → Store → User hierarchy |
| **Image Storage** | AWS S3 with public read |
| **Infrastructure** | AWS ECS Fargate, RDS MySQL, ALB, S3, Secrets Manager |
| **CI/CD** | GitHub Actions (build, test, deploy, terraform) |

---

## 5. Target Market

### 5.1 Market Size (India)

| Metric | Value | Source |
|--------|-------|--------|
| Total jewellery retailers in India | ~500,000 | IBEF, GJC |
| Organized sector (potential SaaS buyers) | ~150,000 | Estimated |
| Maharashtra jewellery retailers | ~50,000 | State data |
| Satara district jewellery retailers | ~500-800 | Local estimate |
| Average annual spend on software | ₹15,000-₹30,000 | Industry survey |
| Total Addressable Market (TAM) | ₹4,500 Crore/year | 150K × ₹30K |
| Serviceable Addressable Market (SAM) | ₹150 Crore/year | Maharashtra |
| Serviceable Obtainable Market (SOM) | ₹50 Lakh/year | Satara + nearby (Year 1) |

### 5.2 Customer Segments

| Segment | Size | Avg. Revenue | Priority |
|---------|------|-------------|----------|
| **Small single-store** (1-2 staff) | 60% of market | ₹8K-₹10K/year | 🟡 Volume play |
| **Medium store** (3-10 staff) | 30% of market | ₹15K-₹20K/year | 🟢 Primary target |
| **Multi-store chain** (10+ staff, 2-5 branches) | 10% of market | ₹30K-₹50K/year | 🟢 High value |

### 5.3 Ideal Customer Profile (ICP)

- Jewellery store in Tier-2/Tier-3 city (Satara, Pune, Kolhapur, Sangli)
- ₹50 Lakh - ₹5 Crore annual turnover
- 2-10 staff members
- Currently using manual register or outdated software
- Owner aged 25-50 (tech-comfortable)
- Has a smartphone (Android)
- Wants to modernize but doesn't want to spend ₹50K+ upfront

---

## 6. Competitive Analysis

### 6.1 Direct Competitors

| Feature | **JewelERP** | RetailJewel | Jewelsoft | BUSY | Vyapar |
|---------|-------------|-------------|-----------|------|--------|
| **Pricing** | ₹799-₹2,999/mo | ₹15K-₹50K/yr | ₹20K-₹60K/yr | ₹12K-₹25K/yr | ₹5K-₹10K/yr |
| **Model** | Cloud SaaS | On-premise | On-premise | License | Cloud |
| **Desktop App** | ✅ Electron | ✅ Windows | ✅ Windows | ✅ Windows | ✅ Web |
| **Admin Mobile App** | ✅ Flutter | ❌ | ❌ | ❌ | ✅ Basic |
| **Customer App** | ✅ Flutter | ❌ | ❌ | ❌ | ❌ |
| **Multi-store** | ✅ Built-in | ❌ or extra | ❌ or extra | ❌ | ❌ |
| **Saving Schemes** | ✅ | ✅ | ✅ | ❌ | ❌ |
| **Image Upload** | ✅ S3 | ❌ | ❌ | ❌ | ✅ Basic |
| **GST Billing** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Real-time Dashboard** | ✅ | Basic | Basic | ❌ | Basic |
| **Bulk SMS/Notifications** | ✅ (upcoming) | ❌ | ❌ | ❌ | ❌ |
| **Cloud Access** | ✅ Anywhere | ❌ Local only | ❌ Local only | ❌ Local only | ✅ |
| **Tech Stack** | Modern (2026) | Legacy (.NET) | Legacy (VB) | Legacy (C++) | Modern |

### 6.2 Competitive Advantages

| Advantage | Why It Matters |
|-----------|---------------|
| **Customer App** | No competitor has this — instant digital presence for jewellers |
| **Cloud-native** | Access from anywhere — not tied to one computer |
| **Multi-store built-in** | Growing jewellers don't need to buy separate licenses |
| **Modern UI** | Young staff prefer modern apps over 1990s-looking software |
| **Monthly pricing** | Low risk — try for ₹799, no ₹50K upfront commitment |
| **Admin Mobile App** | Owner can monitor business from home/travel |

### 6.3 Competitive Moat

1. **Network effect:** More stores on JewelERP → more customers use Customer App → more stores want to join
2. **Data moat:** Years of inventory, billing, customer data → switching cost is high
3. **Feature velocity:** Cloud SaaS = instant updates for all users (vs. on-premise = manual upgrades)

---

## 7. Revenue Model & Pricing

### 7.1 Pricing Plans

#### 🥉 Starter — ₹799/month (₹7,999/year)

**Target:** Small single-store jeweller

| Included | Limit |
|----------|-------|
| 1 Store | — |
| 2 Users (Owner + 1 Staff) | — |
| Inventory Management | Up to 500 items |
| Billing (Invoice, Estimate, Credit Note) | Unlimited |
| Customer Management | Up to 200 customers |
| GST Reports | ✅ |
| Daily Rates | ✅ |
| Old Gold | ✅ |
| Desktop App | ✅ |
| Admin Mobile App | ✅ |
| Customer App | ❌ |
| Saving Schemes | ❌ |
| Image Upload | ❌ |
| Bulk SMS | ❌ |
| Support | Email only |

#### 🥈 Professional — ₹1,499/month (₹14,999/year)

**Target:** Medium jeweller with staff, wants full features

| Included | Limit |
|----------|-------|
| 1 Store | — |
| 5 Users | — |
| Inventory Management | Unlimited |
| All Billing Features | Unlimited |
| Customer Management | Unlimited |
| All Accounts Features | ✅ |
| Saving Schemes | ✅ |
| Dashboard & Reports | ✅ |
| Desktop App | ✅ |
| Admin Mobile App | ✅ |
| Customer App | ✅ |
| Image Upload | Up to 1 GB |
| Bulk SMS | ❌ |
| Support | Email + WhatsApp |

#### 🥇 Enterprise — ₹2,999/month (₹29,999/year)

**Target:** Multi-store jeweller, chains

| Included | Limit |
|----------|-------|
| Unlimited Stores | — |
| Unlimited Users | — |
| Everything in Professional | ✅ |
| Multi-store Dashboard | ✅ |
| Bulk SMS/Notifications | 500 SMS/month included |
| Image Upload | Up to 5 GB |
| Activity Logs & Audit | ✅ |
| Priority Support | Phone + WhatsApp + Email |
| Custom Branding (Customer App) | ✅ |
| Data Export | ✅ |

### 7.2 Add-ons

| Add-on | Price |
|--------|-------|
| Extra Store | ₹499/month per store |
| Extra User | ₹199/month per user |
| Bulk SMS Pack (1,000 SMS) | ₹299 one-time |
| Extra Storage (1 GB) | ₹99/month |
| WhatsApp Business Integration | ₹999/month |
| Onboarding & Training (in-person) | ₹2,999 one-time |

### 7.3 Pricing Summary

```
┌─────────────────────────────────────────────────────────┐
│                    JewelERP Pricing                      │
├───────────────┬────────────────┬─────────────────────────┤
│    Starter    │  Professional  │      Enterprise          │
│   ₹799/mo    │   ₹1,499/mo   │      ₹2,999/mo           │
│  ₹7,999/yr   │  ₹14,999/yr   │     ₹29,999/yr           │
├───────────────┼────────────────┼─────────────────────────┤
│ 1 Store       │ 1 Store        │ Unlimited Stores         │
│ 2 Users       │ 5 Users        │ Unlimited Users          │
│ 500 Items     │ Unlimited      │ Unlimited                │
│ Desktop ✅    │ Desktop ✅     │ Desktop ✅               │
│ Admin App ✅  │ Admin App ✅   │ Admin App ✅             │
│ Cust App ❌   │ Cust App ✅    │ Cust App ✅ + Branding   │
│ Schemes ❌    │ Schemes ✅     │ Schemes ✅               │
│ SMS ❌        │ SMS ❌         │ 500 SMS/mo ✅            │
│ Email support │ Email + WA     │ Phone + WA + Email       │
└───────────────┴────────────────┴─────────────────────────┘
```

### 7.4 Revenue Streams

| Stream | Type | Description |
|--------|------|-------------|
| **Subscription** | Recurring | Monthly/annual plan fees |
| **Add-ons** | Recurring | Extra stores, users, storage |
| **SMS Packs** | Usage-based | Bulk SMS credits |
| **Onboarding** | One-time | In-person setup & training |
| **WhatsApp Integration** | Recurring | Premium add-on |

---

## 8. Cost Analysis

### 8.1 AWS Infrastructure Cost (Per Month)

| Service | Specification | Monthly Cost |
|---------|--------------|-------------|
| ECS Fargate | 0.5 vCPU, 1 GB RAM, 1 task | ~₹800 (≈$10) |
| RDS MySQL | db.t3.micro (Free Tier Year 1) | ₹0 → ₹1,600 (Year 2+) |
| ALB | Application Load Balancer | ~₹1,500 (≈$18) |
| S3 | Image storage (first 5 GB free) | ~₹50 |
| ECR | Docker images | ~₹80 |
| Secrets Manager | 2 secrets | ~₹80 |
| CloudWatch | Logs (first 5 GB free) | ~₹100 |
| Data Transfer | First 100 GB free | ~₹200 |
| **Total (Year 1)** | | **~₹2,800/month** |
| **Total (Year 2+)** | | **~₹4,400/month** |

### 8.2 Scaling Costs

| Customers | ECS Tasks | RDS Size | Estimated AWS Cost |
|-----------|-----------|----------|--------------------|
| 1-20 stores | 1 task | db.t3.micro | ₹2,800-₹4,400/mo |
| 20-50 stores | 2 tasks | db.t3.small | ₹8,000-₹10,000/mo |
| 50-200 stores | 3-4 tasks | db.t3.medium | ₹15,000-₹20,000/mo |
| 200-500 stores | 4-6 tasks | db.r6g.large | ₹35,000-₹50,000/mo |

### 8.3 Other Costs

| Cost | Monthly | Notes |
|------|---------|-------|
| GitHub (Pro) | ₹500 | CI/CD, repo |
| Domain + SSL | ₹100 | Annual domain ≈ ₹1,200 |
| DLT Registration | ₹0 | One-time, free |
| SMS (AWS SNS) | Pay-per-use | ₹0.15-₹0.20 per SMS |
| Apple Developer | ₹8,300/year | For iOS Customer App |
| Google Play | ₹2,100 one-time | For Android apps |

### 8.4 Break-Even Analysis

| Metric | Value |
|--------|-------|
| Fixed monthly cost (AWS + tools) | ~₹5,000 |
| Revenue per Starter customer | ₹799/month |
| Revenue per Professional customer | ₹1,499/month |
| **Break-even** | **4 Professional customers** or **7 Starter customers** |

> **With just 4-7 paying customers, you cover all infrastructure costs.**

---

## 9. Go-to-Market Strategy

### 9.1 Phase 1: Seed (Month 1-3) — Satara

**Goal:** 5-10 free trial customers, validate product-market fit

| Action | Details |
|--------|---------|
| **Personal visits** | Visit 20-30 jewellery stores in Satara personally |
| **Free trial** | Offer 3-month free trial (Professional plan) |
| **Setup service** | Set up the software in-store for free |
| **Feedback loop** | WhatsApp group with trial users for daily feedback |
| **Demo day** | Organize a demo at a popular jeweller's shop |

**Success criteria:** 5+ stores actively using daily, 0 critical bugs

### 9.2 Phase 2: Validate (Month 3-6) — Satara + Karad

**Goal:** 15-25 paying customers, prove willingness to pay

| Action | Details |
|--------|---------|
| **Convert trial users** | Offer ₹999/month introductory price (₹9,999/year) |
| **Referral program** | Existing users get 1 month free for each referral |
| **Expand to Karad** | Nearby town, 30 min from Satara |
| **Case study** | Create a video testimonial from a happy jeweller |
| **Local newspaper ad** | Small ad in Satara Samachar / local daily |

**Success criteria:** 15+ paying customers, <5% monthly churn

### 9.3 Phase 3: Grow (Month 6-12) — Western Maharashtra

**Goal:** 50-100 paying customers

| Action | Details |
|--------|---------|
| **Expand cities** | Pune, Kolhapur, Sangli, Solapur |
| **Hire 1 sales person** | Local person who knows jewellery market |
| **Jewellery association events** | Sponsor/attend Sarafa association meetings |
| **Google Ads** | "Jewellery billing software" — target Maharashtra |
| **YouTube channel** | Tutorial videos in Marathi |
| **Normalize pricing** | Move to ₹1,499/month (Professional) |

**Success criteria:** ₹10 Lakh ARR, 50+ active stores

### 9.4 Phase 4: Scale (Month 12-24) — Pan-Maharashtra + Adjacent States

**Goal:** 200-500 paying customers

| Action | Details |
|--------|---------|
| **Hire 2 more sales people** | One for Pune, one for Vidarbha |
| **Partner with CA firms** | CAs recommend software to jeweller clients |
| **Partner with jewellery suppliers** | Suppliers recommend to their retail network |
| **Launch Customer App on Play Store** | Marketing hook: "Give your customers an app" |
| **SEO + Content** | Blog: "Best jewellery software in India" |
| **Expand to Gujarat, Karnataka** | Hindi/Kannada language support |

**Success criteria:** ₹50 Lakh ARR, 200+ active stores

### 9.5 Sales Pitch Script (Marathi/Hindi)

> *"साहेब, तुमच्या दुकानासाठी एक app बनवलं आहे. तुमचे सगळे items, billing, scheme सगळं एकाच ठिकाणी. आणि तुमच्या customers ला पण एक app मिळतो — ते तुमचे products बघू शकतात, enquiry करू शकतात. महिना फक्त ₹799. तीन महिने free trial. बघा आवडलं तर ठेवा."*

Translation: "Sir, I've built an app for your store. All your items, billing, schemes in one place. And your customers also get an app — they can browse your products and enquire. Only ₹799/month. Three months free trial. Try it, keep it if you like it."

---

## 10. Technology Stack & Architecture

### 10.1 Current Architecture

```
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│  Desktop App     │  │  Admin Mobile    │  │  Customer App    │
│  (Electron.js)   │  │  (Flutter)       │  │  (Flutter)       │
└────────┬─────────┘  └────────┬─────────┘  └────────┬─────────┘
         │                     │                      │
         └─────────────────────┼──────────────────────┘
                               │ HTTPS (JWT Auth)
                               ▼
                      ┌────────────────┐
                      │  AWS ALB        │
                      └────────┬───────┘
                               │
                      ┌────────┴───────┐        ┌─────────────┐
                      │  ECS Fargate    │───────►│  AWS S3      │
                      │  Spring Boot 3  │        │  (Images)    │
                      │  Java 21        │        └─────────────┘
                      └────────┬───────┘
                               │
                      ┌────────┴───────┐        ┌─────────────┐
                      │  RDS MySQL 8    │        │  Secrets Mgr │
                      │  (Database)     │        │  (Creds)     │
                      └────────────────┘        └─────────────┘
```

### 10.2 Tech Decisions & Why

| Decision | Why |
|----------|-----|
| **Java 21 + Spring Boot** | Enterprise-grade, massive ecosystem, easy to hire developers |
| **MySQL** | Jewellery data is relational, MySQL is battle-tested, RDS Free Tier |
| **ECS Fargate** | No server management, auto-scaling, cheaper than EKS |
| **Electron.js** | Cross-platform desktop (Windows + Mac), web tech stack |
| **Flutter** | Single codebase for Android + iOS, fast development |
| **Terraform** | Reproducible infrastructure, version-controlled |
| **GitHub Actions** | Free CI/CD for open-source, tight GitHub integration |
| **Flyway** | Database migrations versioned with code |
| **JWT** | Stateless auth, works across all 3 client apps |
| **S3** | Cheap, reliable, global CDN-ready for images |

### 10.3 API Summary

| Category | Endpoints | Description |
|----------|-----------|-------------|
| Auth | 3 | Login, refresh, change password |
| Staff | 5 | CRUD + permissions |
| Inventory | 12 | Items, categories, metal types |
| Billing | 11 | Invoices, estimates, credit notes |
| Accounts | 14 | Ledger, expenses, cash register, suppliers, old gold |
| Rates | 3 | Daily metal rates |
| Schemes | 8 | Saving schemes + members + payments |
| Dashboard | 1 | Sales summary |
| Settings | 2 | Store settings |
| Image Upload | 3 | S3 upload/delete (admin + customer) |
| Customer App | 14 | Register, catalog, wishlist, enquiry, profile |
| Admin/Health | 5 | DB admin, health checks |
| **Total** | **110** | |

---

## 11. Product Roadmap

### 11.1 Completed (As of March 2026) ✅

| Module | Status |
|--------|--------|
| Multi-tenant auth (Org → Store → User → Customer) | ✅ Done |
| Inventory management with image upload | ✅ Done |
| GST-compliant billing (Invoice, Estimate, Credit Note) | ✅ Done |
| Customer management | ✅ Done |
| Accounts (Ledger, Expenses, Cash Register, Suppliers) | ✅ Done |
| Daily rates management | ✅ Done |
| Old gold tracking | ✅ Done |
| Saving schemes (create, members, payments) | ✅ Done |
| Dashboard & reports | ✅ Done |
| Customer App (catalog, wishlist, enquiry with image) | ✅ Done |
| S3 image upload | ✅ Done |
| AWS infrastructure (Terraform) | ✅ Done |
| CI/CD pipelines (GitHub Actions) | ✅ Done |

### 11.2 Q2 2026 (April - June)

| Feature | Priority | Effort |
|---------|----------|--------|
| Bulk SMS notifications (AWS SNS) | 🔴 High | 2 weeks |
| FCM push notifications (Customer App) | 🔴 High | 1 week |
| Birthday/Anniversary auto-wishes | 🟡 Medium | 3 days |
| Invoice PDF generation & download | 🔴 High | 1 week |
| Barcode scanning integration | 🟡 Medium | 1 week |
| Customer App — scheme payment tracking | 🔴 High | 1 week |
| Activity log improvements | 🟢 Low | 3 days |

### 11.3 Q3 2026 (July - September)

| Feature | Priority | Effort |
|---------|----------|--------|
| WhatsApp Business API integration | 🔴 High | 2 weeks |
| UPI/Payment gateway (for scheme payments) | 🔴 High | 2 weeks |
| Advanced reports (daily, weekly, monthly, custom) | 🟡 Medium | 2 weeks |
| Multi-language support (Marathi, Hindi, Gujarati) | 🟡 Medium | 2 weeks |
| Customer loyalty points | 🟡 Medium | 1 week |
| Store-branded Customer App (white-label) | 🟢 Low | 3 weeks |

### 11.4 Q4 2026 (October - December)

| Feature | Priority | Effort |
|---------|----------|--------|
| HALLMARK integration (BIS) | 🔴 High | 3 weeks |
| Tally/BUSY export | 🟡 Medium | 2 weeks |
| GST return filing helper | 🟡 Medium | 2 weeks |
| AI-powered demand prediction | 🟢 Low | 4 weeks |
| Customer App — appointment booking | 🟢 Low | 1 week |
| Audit trail & compliance reports | 🟡 Medium | 1 week |

### 11.5 2027 Vision

| Feature | Description |
|---------|-------------|
| **B2B Marketplace** | Suppliers can list, retailers can order online |
| **Gold loan management** | Track gold loan given/taken by customers |
| **Insurance integration** | Jewellery insurance partnerships |
| **Analytics dashboard** | AI-powered business insights |
| **Franchise management** | For jewellery chains with 10+ stores |

---

## 12. Team & Roles

### 12.1 Current Team

| Role | Person | Responsibility |
|------|--------|---------------|
| **Founder & Full-Stack Developer** | Raviraj Bhosale | Backend (Spring Boot), infrastructure (AWS/Terraform), architecture |
| **Junior Developer** | [To be assigned] | Scheme module, bug fixes, testing |

### 12.2 Hiring Plan

| Role | When | Monthly Cost | Purpose |
|------|------|-------------|---------|
| Flutter Developer | Month 3 | ₹30K-₹50K | Customer App + Admin Mobile App |
| Frontend Developer (Electron) | Month 3 | ₹30K-₹50K | Desktop App improvements |
| Sales Person (Satara) | Month 6 | ₹15K + commission | Door-to-door jeweller visits |
| Customer Support | Month 9 | ₹15K-₹20K | WhatsApp + phone support |
| Backend Developer | Month 12 | ₹40K-₹60K | Feature development, scaling |

### 12.3 Total Team Cost (Month 12)

| Team | Count | Monthly Cost |
|------|-------|-------------|
| Developers | 3-4 | ₹1.5-₹2 Lakh |
| Sales | 1-2 | ₹30K-₹50K |
| Support | 1 | ₹15K-₹20K |
| **Total** | **5-7** | **₹2-₹2.7 Lakh/month** |

---

## 13. Financial Projections

### 13.1 Year 1 (Month 1-12)

| Month | New Customers | Total Active | MRR | Cumulative Revenue |
|-------|--------------|-------------|-----|-------------------|
| 1-3 | 5-10 (free trial) | 5-10 | ₹0 | ₹0 |
| 4 | 8 paid | 8 | ₹9,592 | ₹9,592 |
| 5 | +5 | 13 | ₹15,587 | ₹25,179 |
| 6 | +5 | 18 | ₹21,582 | ₹46,761 |
| 7 | +7 | 25 | ₹29,975 | ₹76,736 |
| 8 | +7 | 32 | ₹38,368 | ₹1,15,104 |
| 9 | +8 | 40 | ₹47,960 | ₹1,63,064 |
| 10 | +10 | 50 | ₹59,950 | ₹2,23,014 |
| 11 | +10 | 60 | ₹71,940 | ₹2,94,954 |
| 12 | +15 | 75 | ₹89,925 | ₹3,84,879 |

**Assumptions:**
- Average revenue per customer: ₹1,199/month (mix of Starter + Professional)
- Churn: 5% monthly
- Month 1-3: Free trial (no revenue)

### 13.2 Year 1 P&L Summary

| Item | Amount |
|------|--------|
| **Revenue** | **₹3,85,000** |
| AWS Infrastructure | (₹45,000) |
| Tools & Services | (₹15,000) |
| Marketing | (₹30,000) |
| Travel (store visits) | (₹20,000) |
| **Net Profit/Loss (Year 1)** | **₹2,75,000** |

> Year 1 is profitable because you're the sole developer — no salary cost. Infrastructure is minimal.

### 13.3 Year 2 Projection

| Metric | Value |
|--------|-------|
| Active customers (end of Year 2) | 200-300 |
| Average MRR | ₹3-₹4 Lakh |
| Annual Revenue (ARR) | ₹36-₹48 Lakh |
| Team cost | ₹24-₹30 Lakh/year |
| AWS cost | ₹1.5-₹2.5 Lakh/year |
| **Net Profit** | **₹8-₹15 Lakh** |

### 13.4 Year 3 Projection

| Metric | Value |
|--------|-------|
| Active customers | 500-800 |
| ARR | ₹80 Lakh - ₹1.2 Crore |
| Team size | 8-12 |
| Net margin | 25-35% |

---

## 14. Risks & Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| **Jewellers resist change** | High | High | Free trial, in-person demo, solve their biggest pain first (billing) |
| **Internet connectivity** | Medium | High | Offline-first mode in Desktop app (sync when online) — roadmap item |
| **Price sensitivity** | High | Medium | Start at ₹799/month, cheaper than all competitors |
| **Competition launches cloud product** | Medium | Medium | Move fast, build customer relationships, network effect of Customer App |
| **AWS costs increase** | Low | Medium | Architecture is portable — can move to any cloud/VPS |
| **Data security concerns** | Medium | High | SOC2 compliance roadmap, data encryption, customer-visible security page |
| **Key person risk (solo founder)** | High | High | Document everything, hire early, modular codebase |
| **Regulatory changes (GST/BIS)** | Medium | Medium | Stay updated, quick deployments via CI/CD |
| **SMS delivery issues (DLT)** | Medium | Low | FCM as primary, SMS as secondary channel |

---

## 15. Key Metrics (KPIs)

### 15.1 Business Metrics

| Metric | Target (Month 6) | Target (Month 12) | Target (Month 24) |
|--------|------------------|--------------------|--------------------|
| Active paying stores | 20 | 75 | 300 |
| MRR (Monthly Recurring Revenue) | ₹24,000 | ₹90,000 | ₹3,60,000 |
| ARR (Annual Recurring Revenue) | ₹2.9 Lakh | ₹10.8 Lakh | ₹43.2 Lakh |
| Churn rate (monthly) | <8% | <5% | <3% |
| Customer Acquisition Cost (CAC) | ₹500 | ₹1,000 | ₹2,000 |
| Lifetime Value (LTV) | ₹15,000 | ₹25,000 | ₹40,000 |
| LTV:CAC ratio | 30:1 | 25:1 | 20:1 |

### 15.2 Product Metrics

| Metric | Target |
|--------|--------|
| Daily Active Users (DAU) | 60% of total users |
| Invoices created per day (per store) | 5+ |
| Customer App downloads | 10x of store count |
| API uptime | 99.5%+ |
| Average API response time | <500ms |
| NPS (Net Promoter Score) | 40+ |

### 15.3 Technical Metrics

| Metric | Target |
|--------|--------|
| Deployment frequency | 2-3 times/week |
| Mean Time to Recovery (MTTR) | <1 hour |
| Test coverage | >70% |
| P0 bug resolution | <24 hours |

---

## 16. Legal & Compliance

### 16.1 Required Registrations

| Registration | Status | Purpose |
|-------------|--------|---------|
| Company registration (LLP/Pvt Ltd) | 📋 Needed | Legal entity |
| GST Registration | 📋 Needed (after ₹20L revenue) | Tax compliance |
| DLT Registration (TRAI) | 📋 Needed | SMS sending |
| Apple Developer Program | 📋 Needed | iOS app publishing |
| Google Play Console | 📋 Needed | Android app publishing |
| Domain registration | ✅ Done | Website |

### 16.2 Data Privacy & Security

| Measure | Status |
|---------|--------|
| Data encrypted in transit (HTTPS) | ✅ |
| Data encrypted at rest (RDS encryption) | ✅ |
| JWT-based authentication | ✅ |
| Role-based access control | ✅ |
| Secrets in AWS Secrets Manager | ✅ |
| GDPR/Indian Data Protection compliance | 📋 Roadmap |
| Terms of Service | 📋 Needed |
| Privacy Policy | 📋 Needed |
| Data Processing Agreement | 📋 Needed |

### 16.3 Contracts Needed

| Document | Purpose |
|----------|---------|
| **Terms of Service** | User agreement for SaaS service |
| **Privacy Policy** | How customer data is collected, used, stored |
| **SLA (Service Level Agreement)** | Uptime guarantee (99.5%) |
| **Data Processing Agreement** | GDPR/India data protection compliance |
| **Subscription Agreement** | Pricing, billing, cancellation terms |

---

## 17. Exit Strategy / Long-Term Vision

### 17.1 Long-Term Vision (5 Years)

```
Year 1:  Product-market fit in Satara (75 stores)
Year 2:  Expand to Maharashtra (300 stores, ₹40L ARR)
Year 3:  Expand to 5 states (800 stores, ₹1.2Cr ARR)
Year 4:  Pan-India (2000 stores, ₹3.5Cr ARR)
Year 5:  Market leader in jewellery SaaS (5000 stores, ₹8Cr ARR)
```

### 17.2 Potential Exit Options

| Option | When | Valuation Multiple |
|--------|------|-------------------|
| **Bootstrap & profit** | Ongoing | N/A — lifestyle business generating ₹50L-₹1Cr/year profit |
| **Angel/Seed funding** | Year 2 (₹40L ARR) | ₹2-₹5 Crore valuation |
| **Series A** | Year 3-4 (₹1Cr+ ARR) | ₹15-₹30 Crore valuation |
| **Acquisition** | Year 3-5 | By Tally, Zoho, or large jewellery ERP company |
| **Merge** | Year 2-3 | With a complementary product (e.g., jewellery marketplace) |

### 17.3 Strategic Acquirers

| Company | Why They'd Acquire |
|---------|-------------------|
| **Tally Solutions** | Enter jewellery vertical |
| **Zoho** | Expand SMB SaaS portfolio |
| **Mswipe/Pine Labs** | Add POS + ERP for jewellers |
| **CaratLane/Tanishq** | Technology for franchise stores |
| **A large jewellery chain** | In-house ERP for all branches |

---

## Appendix: One-Page Summary

```
┌──────────────────────────────────────────────────────────────────┐
│                         JewelERP                                  │
│         Cloud-Native Jewellery Management System                  │
├──────────────────────────────────────────────────────────────────┤
│                                                                   │
│  PROBLEM:  500K+ Indian jewellers use manual/outdated systems     │
│  SOLUTION: Cloud SaaS with Desktop + Admin Mobile + Customer App  │
│  MARKET:   ₹4,500 Cr TAM (India jewellery software)              │
│                                                                   │
│  PRODUCT:  110 APIs, 33+ DB tables, AWS cloud-native              │
│            3 client apps: Electron + Flutter + Flutter             │
│                                                                   │
│  PRICING:  ₹799/mo (Starter) → ₹1,499/mo → ₹2,999/mo            │
│  REVENUE:  ₹3.85L (Y1) → ₹40L (Y2) → ₹1.2Cr (Y3)              │
│  MARGIN:   70%+ (SaaS)                                           │
│                                                                   │
│  MOAT:     Customer App (no competitor has this)                  │
│            Network effect: More stores → More customers → More $  │
│            Data lock-in: Years of inventory + billing data        │
│                                                                   │
│  TEAM:     Solo founder (full-stack) → 5-7 by Month 12           │
│  ASK:      No funding needed — bootstrapped, profitable by M4     │
│                                                                   │
│  VISION:   Market leader in Indian jewellery SaaS by 2030        │
│            5,000 stores, ₹8 Cr ARR                               │
│                                                                   │
└──────────────────────────────────────────────────────────────────┘
```
