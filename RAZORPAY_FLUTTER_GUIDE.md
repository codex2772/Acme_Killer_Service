# JewelERP — Razorpay Flutter Integration Guide

> **Base URL:** `http://jewel-erp-alb-2124014483.ap-south-1.elb.amazonaws.com`
>
> **Auth:** All payment endpoints require `Authorization: Bearer <customer_jwt_token>`
> except the webhook (server-to-server, no JWT).
>
> **Date:** April 1, 2026

---

## Table of Contents

1. [Quick Start — Flutter Setup](#1-quick-start--flutter-setup)
2. [Payment Flow Overview](#2-payment-flow-overview)
3. [API Endpoints](#3-api-endpoints)
   - [3.1 Create Payment Order](#31-create-payment-order)
   - [3.2 Verify Payment](#32-verify-payment)
   - [3.3 Payment History](#33-payment-history)
4. [Complete Flutter Code](#4-complete-flutter-code)
5. [Test Credentials](#5-test-credentials)
6. [Error Handling](#6-error-handling)
7. [Testing Checklist](#7-testing-checklist)

---

## 1. Quick Start — Flutter Setup

### 1.1 Add Dependency

```yaml
# pubspec.yaml
dependencies:
  razorpay_flutter: ^1.3.7
  http: ^1.2.0
```

```bash
flutter pub get
```

### 1.2 Android Setup

Add internet permission in `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

In `android/app/build.gradle`, ensure `minSdkVersion` is at least 19:

```gradle
defaultConfig {
    minSdkVersion 19
    // ...
}
```

### 1.3 iOS Setup

No additional setup needed — Razorpay Flutter SDK handles iOS automatically.

---

## 2. Payment Flow Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                     PAYMENT FLOW (3 Steps)                          │
│                                                                     │
│  Step 1: Create Order                                               │
│  ─────────────────────                                              │
│  Flutter → POST /api/customer-app/schemes/{memberId}/pay → Backend  │
│  Backend → Razorpay API (create order) → Backend                    │
│  Backend → { orderId, amount, razorpayKeyId, ... } → Flutter        │
│                                                                     │
│  Step 2: Open Razorpay Checkout                                     │
│  ──────────────────────────────                                     │
│  Flutter → Razorpay SDK .open(options) → Customer pays              │
│  Razorpay → { paymentId, orderId, signature } → Flutter callback    │
│                                                                     │
│  Step 3: Verify Payment                                             │
│  ──────────────────────                                             │
│  Flutter → POST /api/customer-app/payments/verify → Backend         │
│  Backend → Verifies HMAC signature → Creates SchemePayment          │
│  Backend → { success: true, monthNumber, ... } → Flutter            │
│                                                                     │
│  ✅ Done! Customer's month is marked as PAID.                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 3. API Endpoints

### 3.1 Create Payment Order

Creates a Razorpay order for the **next unpaid month** of a scheme membership. Returns all details needed by the Flutter Razorpay SDK to open checkout.

```
POST /api/customer-app/schemes/{memberId}/pay
```

**Headers:**

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <customer_jwt_token>` | ✅ |
| `Content-Type` | `application/json` | ✅ |

**Path Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `memberId` | Long | Scheme membership ID (from `GET /api/customer-app/schemes`) |

**Request Body:** Empty `{}` or no body needed.

**Success Response:** `200 OK`

```json
{
  "orderId": "order_NxP1abc2defGH",
  "amount": 500000,
  "currency": "INR",
  "razorpayKeyId": "rzp_test_xxxxxxxxx",
  "customerName": "Priya Sharma",
  "customerPhone": "9876543210",
  "customerEmail": "priya@example.com",
  "schemeName": "Gold Saving Scheme 2026",
  "monthNumber": 4,
  "description": "Gold Saving Scheme 2026 - Month 4 Payment"
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `orderId` | String | Razorpay order ID — **pass directly to Razorpay SDK** |
| `amount` | Long | Amount in **paise** (500000 = ₹5,000) — **pass directly to Razorpay SDK** |
| `currency` | String | Always `"INR"` — **pass directly to Razorpay SDK** |
| `razorpayKeyId` | String | Razorpay public key — **pass as `key` to Razorpay SDK** |
| `customerName` | String | Pre-fill in checkout `prefill.name` |
| `customerPhone` | String | Pre-fill in checkout `prefill.contact` |
| `customerEmail` | String | Pre-fill in checkout `prefill.email` (can be `null`) |
| `schemeName` | String | Display to user / use as checkout `name` |
| `monthNumber` | Integer | Which month this payment is for (1-based) |
| `description` | String | Human-readable — use as checkout `description` |

**Error Responses:**

| Status | Body | When |
|--------|------|------|
| `400` | `{"message": "Scheme membership not found or does not belong to you"}` | Invalid memberId or not your membership |
| `400` | `{"message": "This membership is not active"}` | Membership is COMPLETED/DEFAULTED |
| `400` | `{"message": "All months are already paid for this scheme"}` | All installments done |
| `500` | `{"message": "Failed to create payment order: ..."}` | Razorpay API error |

**Duplicate Order Prevention:** If you call this API twice within 15 minutes for the same month, it returns the **same order** (doesn't create a duplicate). After 15 minutes, a new order is created.

---

### 3.2 Verify Payment

Called by Flutter **after** the Razorpay checkout success callback. Sends the Razorpay payment details to the backend for HMAC signature verification.

```
POST /api/customer-app/payments/verify
```

**Headers:**

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <customer_jwt_token>` | ✅ |
| `Content-Type` | `application/json` | ✅ |

**Request Body:**

```json
{
  "razorpayOrderId": "order_NxP1abc2defGH",
  "razorpayPaymentId": "pay_NxP2xyz9abcDE",
  "razorpaySignature": "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6abcd"
}
```

**Request Fields:**

| Field | Type | Required | Description | Source |
|-------|------|----------|-------------|--------|
| `razorpayOrderId` | String | ✅ | Order ID | `PaymentSuccessResponse.orderId` |
| `razorpayPaymentId` | String | ✅ | Payment ID | `PaymentSuccessResponse.paymentId` |
| `razorpaySignature` | String | ✅ | HMAC signature | `PaymentSuccessResponse.signature` |

> **Important:** All 3 values come directly from the Razorpay Flutter SDK's `PaymentSuccessResponse` callback. Do NOT modify them — pass them exactly as received.

**Success Response:** `200 OK`

```json
{
  "success": true,
  "message": "Payment verified successfully",
  "schemePaymentId": 104,
  "razorpayPaymentId": "pay_NxP2xyz9abcDE",
  "status": "PAID",
  "amount": 5000.00,
  "monthNumber": 4,
  "paymentMethod": "upi",
  "paymentDate": null,
  "paidVia": null
}
```

**Failed Response:** `200 OK` (check `success` field)

```json
{
  "success": false,
  "message": "Payment signature verification failed",
  "schemePaymentId": null,
  "razorpayPaymentId": null,
  "status": "FAILED",
  "amount": null,
  "monthNumber": null,
  "paymentMethod": null,
  "paymentDate": null,
  "paidVia": null
}
```

**Idempotent:** If you call verify twice with the same orderId, the second call returns:

```json
{
  "success": true,
  "message": "Payment already verified",
  "status": "PAID",
  ...
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `success` | boolean | `true` = payment verified, `false` = failed |
| `message` | String | Human-readable message for the user |
| `schemePaymentId` | Long | ID of the created scheme payment record (null if failed) |
| `razorpayPaymentId` | String | Razorpay payment ID (null if failed) |
| `status` | String | `"PAID"` or `"FAILED"` |
| `amount` | BigDecimal | Amount in rupees (e.g., `5000.00`) |
| `monthNumber` | Integer | Which month was paid (e.g., `4`) |
| `paymentMethod` | String | Payment method used: `"upi"`, `"card"`, `"netbanking"`, `"wallet"` |
| `paymentDate` | String | Date of payment (ISO format, may be null) |
| `paidVia` | String | `"online"` or `"store"` (may be null in verify response) |

---

### 3.3 Payment History

Returns all payments for a scheme membership — both **cash** (recorded by admin in desktop app) and **online** (paid via Razorpay). Also includes **PENDING** entries for unpaid months.

```
GET /api/customer-app/schemes/{memberId}/payments
```

**Headers:**

| Header | Value | Required |
|--------|-------|----------|
| `Authorization` | `Bearer <customer_jwt_token>` | ✅ |

**Path Parameters:**

| Param | Type | Description |
|-------|------|-------------|
| `memberId` | Long | Scheme membership ID |

**Success Response:** `200 OK`

```json
[
  {
    "success": true,
    "message": null,
    "schemePaymentId": 101,
    "razorpayPaymentId": null,
    "status": "PAID",
    "amount": 5000.00,
    "monthNumber": 1,
    "paymentMethod": "CASH",
    "paymentDate": "2026-01-15",
    "paidVia": "store"
  },
  {
    "success": true,
    "message": null,
    "schemePaymentId": 102,
    "razorpayPaymentId": "pay_abc123xyz",
    "status": "PAID",
    "amount": 5000.00,
    "monthNumber": 2,
    "paymentMethod": "upi",
    "paymentDate": "2026-02-15",
    "paidVia": "online"
  },
  {
    "success": true,
    "message": null,
    "schemePaymentId": 103,
    "razorpayPaymentId": "pay_def456uvw",
    "status": "PAID",
    "amount": 5000.00,
    "monthNumber": 3,
    "paymentMethod": "card",
    "paymentDate": "2026-03-15",
    "paidVia": "online"
  },
  {
    "success": false,
    "message": null,
    "schemePaymentId": null,
    "razorpayPaymentId": null,
    "status": "PENDING",
    "amount": 5000.00,
    "monthNumber": 4,
    "paymentMethod": null,
    "paymentDate": null,
    "paidVia": null
  },
  {
    "success": false,
    "message": null,
    "schemePaymentId": null,
    "razorpayPaymentId": null,
    "status": "PENDING",
    "amount": 5000.00,
    "monthNumber": 5,
    "paymentMethod": null,
    "paymentDate": null,
    "paidVia": null
  }
]
```

**How to Read the Response:**

| `status` | `paidVia` | Meaning |
|----------|-----------|---------|
| `PAID` | `store` | Admin recorded this payment at the store (cash/counter) |
| `PAID` | `online` | Customer paid via Razorpay (UPI/card/netbanking/wallet) |
| `PENDING` | `null` | Not yet paid — show "Pay Now" button |

**Flutter UI Mapping:**

```dart
for (var payment in paymentHistory) {
  if (payment['status'] == 'PAID') {
    // Show green checkmark ✅
    // Show: "Month ${payment['monthNumber']} — ₹${payment['amount']} — ${payment['paidVia']}"
    // If paidVia == 'online': show Razorpay icon
    // If paidVia == 'store': show cash/store icon
  } else if (payment['status'] == 'PENDING') {
    // Show grey/pending icon
    // Show: "Month ${payment['monthNumber']} — ₹${payment['amount']} — Due"
    // First PENDING month: show [Pay ₹X Now] button
  }
}
```

---

## 4. Complete Flutter Code

### 4.1 Payment Service (Dart)

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:razorpay_flutter/razorpay_flutter.dart';

class RazorpayPaymentService {
  static const String baseUrl =
      'http://jewel-erp-alb-2124014483.ap-south-1.elb.amazonaws.com';

  final String token;
  late Razorpay _razorpay;

  // Callbacks
  Function(String message)? onSuccess;
  Function(String message)? onError;

  RazorpayPaymentService({required this.token}) {
    _razorpay = Razorpay();
    _razorpay.on(Razorpay.EVENT_PAYMENT_SUCCESS, _handleSuccess);
    _razorpay.on(Razorpay.EVENT_PAYMENT_ERROR, _handleError);
    _razorpay.on(Razorpay.EVENT_EXTERNAL_WALLET, _handleExternalWallet);
  }

  /// Step 1: Call backend to create Razorpay order
  Future<void> initiatePayment(int memberId) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/customer-app/schemes/$memberId/pay'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
      );

      if (response.statusCode != 200) {
        final error = jsonDecode(response.body);
        onError?.call(error['message'] ?? 'Failed to create order');
        return;
      }

      final data = jsonDecode(response.body);

      /// Step 2: Open Razorpay checkout with order details
      var options = {
        'key': data['razorpayKeyId'],
        'amount': data['amount'],
        'currency': data['currency'],
        'order_id': data['orderId'],
        'name': 'JewelERP',
        'description': data['description'],
        'prefill': {
          'name': data['customerName'] ?? '',
          'contact': data['customerPhone'] ?? '',
          'email': data['customerEmail'] ?? '',
        },
        'theme': {
          'color': '#D4AF37', // Gold color
        },
        'modal': {
          'confirm_close': true, // Ask before closing checkout
        },
      };

      _razorpay.open(options);
    } catch (e) {
      onError?.call('Error: $e');
    }
  }

  /// Step 3: Payment success → verify with backend
  void _handleSuccess(PaymentSuccessResponse response) async {
    try {
      final verifyResponse = await http.post(
        Uri.parse('$baseUrl/api/customer-app/payments/verify'),
        headers: {
          'Authorization': 'Bearer $token',
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'razorpayOrderId': response.orderId,
          'razorpayPaymentId': response.paymentId,
          'razorpaySignature': response.signature,
        }),
      );

      final result = jsonDecode(verifyResponse.body);

      if (result['success'] == true) {
        onSuccess?.call(
          'Payment of ₹${result['amount']} for Month ${result['monthNumber']} verified!',
        );
      } else {
        onError?.call(
          result['message'] ?? 'Payment received but verification failed. Contact support.',
        );
      }
    } catch (e) {
      // Payment was successful at Razorpay but verify call failed
      // Webhook will handle it as backup
      onError?.call(
        'Payment received. Verification pending — it will be confirmed automatically.',
      );
    }
  }

  /// Payment failed
  void _handleError(PaymentFailureResponse response) {
    String message = 'Payment failed';
    if (response.message != null) {
      try {
        final error = jsonDecode(response.message!);
        message = error['error']?['description'] ?? 'Payment failed';
      } catch (_) {
        message = response.message!;
      }
    }
    onError?.call(message);
  }

  /// External wallet (Paytm, PhonePe, etc.)
  void _handleExternalWallet(ExternalWalletResponse response) {
    // Wallet selected — payment will complete via wallet app
    // No action needed here — success/error callback will fire after
  }

  /// Get payment history for a membership
  Future<List<Map<String, dynamic>>> getPaymentHistory(int memberId) async {
    final response = await http.get(
      Uri.parse('$baseUrl/api/customer-app/schemes/$memberId/payments'),
      headers: {'Authorization': 'Bearer $token'},
    );

    if (response.statusCode == 200) {
      return List<Map<String, dynamic>>.from(jsonDecode(response.body));
    }
    return [];
  }

  /// Dispose Razorpay instance
  void dispose() {
    _razorpay.clear();
  }
}
```

### 4.2 Usage in a Widget

```dart
class SchemeDetailScreen extends StatefulWidget {
  final int memberId;
  final String token;

  const SchemeDetailScreen({
    Key? key,
    required this.memberId,
    required this.token,
  }) : super(key: key);

  @override
  State<SchemeDetailScreen> createState() => _SchemeDetailScreenState();
}

class _SchemeDetailScreenState extends State<SchemeDetailScreen> {
  late RazorpayPaymentService _paymentService;
  List<Map<String, dynamic>> _paymentHistory = [];
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _paymentService = RazorpayPaymentService(token: widget.token);

    _paymentService.onSuccess = (message) {
      setState(() => _isLoading = false);
      _showDialog('✅ Payment Successful', message);
      _loadPaymentHistory(); // Refresh the list
    };

    _paymentService.onError = (message) {
      setState(() => _isLoading = false);
      _showDialog('❌ Payment Failed', message);
    };

    _loadPaymentHistory();
  }

  Future<void> _loadPaymentHistory() async {
    final history = await _paymentService.getPaymentHistory(widget.memberId);
    setState(() => _paymentHistory = history);
  }

  void _payNow() {
    setState(() => _isLoading = true);
    _paymentService.initiatePayment(widget.memberId);
  }

  void _showDialog(String title, String message) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _paymentService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // Find the next pending month
    final nextPending = _paymentHistory.firstWhere(
      (p) => p['status'] == 'PENDING',
      orElse: () => {},
    );

    return Scaffold(
      appBar: AppBar(title: const Text('Scheme Payments')),
      body: Column(
        children: [
          // Pay Now Button
          if (nextPending.isNotEmpty)
            Padding(
              padding: const EdgeInsets.all(16),
              child: ElevatedButton(
                onPressed: _isLoading ? null : _payNow,
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFFD4AF37),
                  minimumSize: const Size(double.infinity, 50),
                ),
                child: _isLoading
                    ? const CircularProgressIndicator(color: Colors.white)
                    : Text(
                        'Pay ₹${nextPending['amount']} — Month ${nextPending['monthNumber']}',
                        style: const TextStyle(fontSize: 18),
                      ),
              ),
            ),

          // Payment History List
          Expanded(
            child: ListView.builder(
              itemCount: _paymentHistory.length,
              itemBuilder: (context, index) {
                final payment = _paymentHistory[index];
                final isPaid = payment['status'] == 'PAID';

                return ListTile(
                  leading: Icon(
                    isPaid ? Icons.check_circle : Icons.radio_button_unchecked,
                    color: isPaid ? Colors.green : Colors.grey,
                  ),
                  title: Text('Month ${payment['monthNumber']}'),
                  subtitle: Text(
                    isPaid
                        ? '₹${payment['amount']} — ${payment['paidVia'] == 'online' ? 'Paid Online' : 'Paid at Store'}'
                        : '₹${payment['amount']} — Due',
                  ),
                  trailing: isPaid
                      ? Text(
                          payment['paymentDate'] ?? '',
                          style: const TextStyle(color: Colors.grey, fontSize: 12),
                        )
                      : null,
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
```

---

## 5. Test Credentials

### Razorpay Test Mode

When using `rzp_test_*` keys, use these test credentials in the Razorpay checkout:

#### ✅ Test Card (Success)

| Field | Value |
|-------|-------|
| Card Number | `4111 1111 1111 1111` |
| Expiry | Any future date (e.g., `12/30`) |
| CVV | Any 3 digits (e.g., `123`) |
| Name | Any name |
| OTP | `1234` (on OTP screen) |

#### ✅ Test UPI (Success)

| Field | Value |
|-------|-------|
| UPI ID | `success@razorpay` |

#### ❌ Test UPI (Failure)

| Field | Value |
|-------|-------|
| UPI ID | `failure@razorpay` |

#### ✅ Test Net Banking

| Field | Value |
|-------|-------|
| Bank | Select any bank |
| Action | Click "Success" on the test bank page |

#### ✅ Test Wallet

| Field | Value |
|-------|-------|
| Wallet | Select any wallet |
| Action | Click "Success" on the test wallet page |

---

## 6. Error Handling

### Error Scenarios & Recommended UI

| Scenario | API Response | Flutter Action |
|----------|-------------|----------------|
| **No internet** | Network exception | Show "No internet connection. Try again." |
| **All months paid** | `400: "All months are already paid"` | Hide "Pay Now" button, show "All Paid ✅" |
| **Membership not active** | `400: "This membership is not active"` | Show "This scheme is no longer active" |
| **Customer pays, then closes app before verify** | Verify never called | **Webhook handles it** — payment auto-confirms within minutes |
| **Signature verification fails** | `verify → success: false` | Show "Payment issue. Contact store. Ref: {orderId}" |
| **Razorpay checkout cancelled by user** | `PaymentFailureResponse` | Show "Payment cancelled" — allow retry |
| **Card declined** | `PaymentFailureResponse` | Show error from Razorpay (e.g., "Insufficient funds") |
| **Duplicate tap on Pay button** | Returns same order (15-min window) | Safe — same checkout opens |

### Retry Logic

```dart
// If createOrder fails with 500, retry once after 2 seconds
if (response.statusCode == 500) {
  await Future.delayed(Duration(seconds: 2));
  // Retry the same POST request
}
```

### Webhook Backup

If the Flutter verify call fails (network drop, app crash), the Razorpay **webhook** will automatically process the payment server-side within seconds. The customer can:

1. Close and reopen the app
2. Check `GET /api/customer-app/schemes/{memberId}/payments`
3. The payment will show as `PAID` (processed by webhook)

---

## 7. Testing Checklist

### Quick Test (Bruno / Postman)

```
Step 1: Login as customer
────────────────────────
POST /api/customer-app/auth/login
Body: {"phone": "<customer_phone>", "password": "<password>"}
→ Save the token

Step 2: Get my scheme memberships
─────────────────────────────────
GET /api/customer-app/schemes
Headers: Authorization: Bearer <token>
→ Note the memberId

Step 3: Create payment order
────────────────────────────
POST /api/customer-app/schemes/{memberId}/pay
Headers: Authorization: Bearer <token>
→ Should return orderId, amount, razorpayKeyId

Step 4: Check payment history
─────────────────────────────
GET /api/customer-app/schemes/{memberId}/payments
Headers: Authorization: Bearer <token>
→ Should show PAID + PENDING months

Step 5: Test webhook endpoint
─────────────────────────────
POST /api/webhooks/razorpay
Body: {"event": "payment.captured", "payload": {"payment": {"entity": {"id": "test", "order_id": "test"}}}}
→ Should return {"status": "ignored", "reason": "missing signature"} (confirms endpoint is live)
```

### Full E2E Test (Flutter App)

```
□  1. Login as customer in Flutter app
□  2. Navigate to My Schemes
□  3. Tap "Pay ₹X" on a scheme
□  4. Razorpay checkout opens
□  5. Enter test UPI: success@razorpay
□  6. Payment succeeds → verify API called
□  7. Success screen shows "Month X paid"
□  8. Payment history refreshes — month shows as PAID (online)
□  9. Tap "Pay ₹X" again → next month's order created
□  10. Cancel payment → error screen → month stays PENDING
□  11. Use failure@razorpay → error shown → month stays PENDING
□  12. Check admin desktop → payment visible with "Online (Razorpay)" label
```

---

## API Quick Reference

| # | Method | Endpoint | Auth | Description |
|---|--------|----------|------|-------------|
| 1 | `POST` | `/api/customer-app/schemes/{memberId}/pay` | 🔓 Customer JWT | Create Razorpay order for next unpaid month |
| 2 | `POST` | `/api/customer-app/payments/verify` | 🔓 Customer JWT | Verify payment after Razorpay SDK callback |
| 3 | `GET` | `/api/customer-app/schemes/{memberId}/payments` | 🔓 Customer JWT | Payment history (online + cash + pending) |
| 4 | `POST` | `/api/webhooks/razorpay` | 🌐 Webhook signature | Server-to-server backup (Razorpay calls this) |

---

*Document generated for JewelERP — Razorpay Flutter Integration Guide*
