/*
 * MIT License
 *
 * Copyright (c) 2026 AuraJewels (Raviraj Bhosale)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.aurajewels.jewel.service;

import com.aurajewels.jewel.dto.customerapp.CreatePaymentOrderResponse;
import com.aurajewels.jewel.dto.customerapp.PaymentStatusResponse;
import com.aurajewels.jewel.dto.customerapp.VerifyPaymentRequest;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling Razorpay payment gateway operations: order creation, payment verification,
 * webhook processing, and payment history retrieval.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayPaymentRepository razorpayPaymentRepository;
    private final SchemeMemberRepository schemeMemberRepository;
    private final SchemePaymentRepository schemePaymentRepository;
    private final CustomerRepository customerRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    // ════════════════════════════════════════
    // 1. CREATE ORDER
    // ════════════════════════════════════════

    /**
     * Creates a Razorpay order for the next unpaid month of a scheme membership. Validates
     * ownership, determines the next month to pay, and calls the Razorpay Orders API.
     */
    @Transactional
    public CreatePaymentOrderResponse createOrder(Long customerId, Long memberId) {
        // 1. Validate membership belongs to this customer
        SchemeMember member =
                schemeMemberRepository
                        .findById(memberId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Scheme membership not found"));

        if (member.getCustomer() == null || !member.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "Scheme membership not found or does not belong to you");
        }

        if (member.getStatus() != SchemeMember.MemberStatus.ACTIVE) {
            throw new IllegalArgumentException("This membership is not active");
        }

        Scheme scheme = member.getScheme();
        Customer customer = member.getCustomer();

        // 2. Find next unpaid month
        List<SchemePayment> paidPayments = schemePaymentRepository.findByMember_Id(memberId);
        Set<Integer> paidMonths =
                paidPayments.stream()
                        .filter(p -> p.getStatus() == SchemePayment.PaymentStatus.PAID)
                        .map(SchemePayment::getMonthNumber)
                        .collect(Collectors.toSet());

        int nextMonth = -1;
        for (int m = 1; m <= scheme.getDurationMonths(); m++) {
            if (!paidMonths.contains(m)) {
                nextMonth = m;
                break;
            }
        }

        if (nextMonth == -1) {
            throw new IllegalArgumentException("All months are already paid for this scheme");
        }

        // 3. Check for recent in-progress order (prevent duplicate orders within 15 min)
        Instant fifteenMinAgo = Instant.now().minus(15, ChronoUnit.MINUTES);
        Optional<RazorpayPayment> recentOrder =
                razorpayPaymentRepository
                        .findBySchemeMember_IdAndMonthNumberAndStatusAndCreatedAtAfter(
                                memberId,
                                nextMonth,
                                RazorpayPayment.RazorpayStatus.CREATED,
                                fifteenMinAgo);

        if (recentOrder.isPresent()) {
            // Return the existing order instead of creating a new one
            RazorpayPayment existing = recentOrder.get();
            return buildOrderResponse(existing, scheme, customer);
        }

        // 4. Create Razorpay order via API
        long amountPaise = scheme.getMonthlyAmount().multiply(BigDecimal.valueOf(100)).longValue();
        String receipt = "scheme_" + memberId + "_month_" + nextMonth;

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);
            orderRequest.put(
                    "notes",
                    new JSONObject()
                            .put("scheme_name", scheme.getName())
                            .put("member_id", memberId)
                            .put("month_number", nextMonth)
                            .put("customer_id", customerId));

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            // 5. Persist tracking record
            RazorpayPayment rpPayment =
                    RazorpayPayment.builder()
                            .schemeMember(member)
                            .razorpayOrderId(orderId)
                            .amountPaise(amountPaise)
                            .currency("INR")
                            .monthNumber(nextMonth)
                            .status(RazorpayPayment.RazorpayStatus.CREATED)
                            .customerId(customerId)
                            .storeId(scheme.getStore().getId())
                            .build();
            rpPayment.setActive(true);
            razorpayPaymentRepository.save(rpPayment);

            log.info(
                    "Razorpay order created: orderId={}, memberId={}, month={}, amount={}",
                    orderId,
                    memberId,
                    nextMonth,
                    amountPaise);

            return buildOrderResponse(rpPayment, scheme, customer);

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment order: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════
    // 2. VERIFY PAYMENT (Client Callback)
    // ════════════════════════════════════════

    /**
     * Verifies the Razorpay payment signature after the Flutter SDK callback. On success, creates a
     * SchemePayment record and marks the Razorpay payment as PAID.
     */
    @Transactional
    public PaymentStatusResponse verifyPayment(Long customerId, VerifyPaymentRequest request) {
        // 1. Find the order
        RazorpayPayment rpPayment =
                razorpayPaymentRepository
                        .findByRazorpayOrderId(request.getRazorpayOrderId())
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Payment order not found: "
                                                        + request.getRazorpayOrderId()));

        // Validate customer owns this order
        if (!rpPayment.getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Payment order does not belong to you");
        }

        // Idempotent: already processed
        if (rpPayment.getStatus() == RazorpayPayment.RazorpayStatus.PAID) {
            BigDecimal amount =
                    BigDecimal.valueOf(rpPayment.getAmountPaise()).divide(BigDecimal.valueOf(100));
            return PaymentStatusResponse.builder()
                    .success(true)
                    .message("Payment already verified")
                    .schemePaymentId(
                            rpPayment.getSchemePayment() != null
                                    ? rpPayment.getSchemePayment().getId()
                                    : null)
                    .razorpayPaymentId(rpPayment.getRazorpayPaymentId())
                    .status("PAID")
                    .amount(amount)
                    .monthNumber(rpPayment.getMonthNumber())
                    .paymentMethod(rpPayment.getPaymentMethod())
                    .build();
        }

        // 2. Verify HMAC-SHA256 signature
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (!isValid) {
                rpPayment.setStatus(RazorpayPayment.RazorpayStatus.FAILED);
                rpPayment.setFailureReason("Signature verification failed");
                razorpayPaymentRepository.save(rpPayment);

                return PaymentStatusResponse.builder()
                        .success(false)
                        .message("Payment signature verification failed")
                        .status("FAILED")
                        .build();
            }
        } catch (RazorpayException e) {
            log.error("Razorpay signature verification error: {}", e.getMessage(), e);
            rpPayment.setStatus(RazorpayPayment.RazorpayStatus.FAILED);
            rpPayment.setFailureReason("Signature verification error: " + e.getMessage());
            razorpayPaymentRepository.save(rpPayment);

            return PaymentStatusResponse.builder()
                    .success(false)
                    .message("Payment verification error")
                    .status("FAILED")
                    .build();
        }

        // 3. Signature valid → mark PAID and create SchemePayment
        return markPaymentSuccess(
                rpPayment, request.getRazorpayPaymentId(), request.getRazorpaySignature(), null);
    }

    // ════════════════════════════════════════
    // 3. WEBHOOK HANDLER (Server-to-Server)
    // ════════════════════════════════════════

    /**
     * Handles Razorpay webhook events (payment.captured, payment.failed). Verifies webhook
     * signature and processes the event idempotently.
     */
    @Transactional
    public void handleWebhook(String payload, String signature) {
        // Skip if webhook secret is not configured yet
        if (webhookSecret == null
                || webhookSecret.isBlank()
                || "webhook_secret_placeholder".equals(webhookSecret)) {
            log.warn(
                    "Razorpay webhook secret not configured — skipping webhook processing");
            return;
        }

        // 1. Verify webhook signature
        try {
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isValid) {
                log.warn("Invalid Razorpay webhook signature — rejecting");
                return;
            }
        } catch (RazorpayException e) {
            log.error("Webhook signature verification error: {}", e.getMessage());
            return;
        }

        // 2. Parse event
        JSONObject event = new JSONObject(payload);
        String eventType = event.optString("event", "");

        JSONObject paymentEntity =
                event.optJSONObject("payload").optJSONObject("payment").optJSONObject("entity");

        if (paymentEntity == null) {
            log.warn("Webhook payload missing payment entity");
            return;
        }

        String orderId = paymentEntity.optString("order_id", "");
        String paymentId = paymentEntity.optString("id", "");
        String method = paymentEntity.optString("method", "");

        log.info(
                "Razorpay webhook received: event={}, orderId={}, paymentId={}",
                eventType,
                orderId,
                paymentId);

        // 3. Find our order
        Optional<RazorpayPayment> optPayment =
                razorpayPaymentRepository.findByRazorpayOrderId(orderId);

        if (optPayment.isEmpty()) {
            log.warn("Webhook for unknown order: {}", orderId);
            return;
        }

        RazorpayPayment rpPayment = optPayment.get();

        // 4. Process based on event type
        switch (eventType) {
            case "payment.captured":
            case "payment.authorized":
                if (rpPayment.getStatus() == RazorpayPayment.RazorpayStatus.PAID) {
                    log.info("Order {} already PAID — webhook idempotent skip", orderId);
                    return;
                }
                markPaymentSuccess(rpPayment, paymentId, null, method);
                log.info("Webhook: Payment marked PAID for order {}", orderId);
                break;

            case "payment.failed":
                if (rpPayment.getStatus() == RazorpayPayment.RazorpayStatus.PAID) {
                    log.info("Order {} already PAID — ignoring failed webhook", orderId);
                    return;
                }
                String reason =
                        paymentEntity.has("error_description")
                                ? paymentEntity.optString("error_description")
                                : "Payment failed";
                rpPayment.setStatus(RazorpayPayment.RazorpayStatus.FAILED);
                rpPayment.setRazorpayPaymentId(paymentId);
                rpPayment.setFailureReason(reason);
                rpPayment.setPaymentMethod(method);
                razorpayPaymentRepository.save(rpPayment);
                log.info("Webhook: Payment marked FAILED for order {}", orderId);
                break;

            default:
                log.info("Unhandled webhook event type: {}", eventType);
        }
    }

    // ════════════════════════════════════════
    // 4. PAYMENT HISTORY
    // ════════════════════════════════════════

    /**
     * Returns payment history for a scheme membership, combining both admin-recorded (CASH)
     * payments and online (Razorpay) payments.
     */
    @Transactional(readOnly = true)
    public List<PaymentStatusResponse> getPaymentHistory(Long customerId, Long memberId) {
        // Validate membership belongs to customer
        SchemeMember member =
                schemeMemberRepository
                        .findById(memberId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Scheme membership not found"));

        if (member.getCustomer() == null || !member.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "Scheme membership not found or does not belong to you");
        }

        Scheme scheme = member.getScheme();

        // Get all scheme payments (both cash and online)
        List<SchemePayment> schemePayments = schemePaymentRepository.findByMember_Id(memberId);

        // Build a map of paid months
        List<PaymentStatusResponse> history = new ArrayList<>();

        for (SchemePayment sp : schemePayments) {
            history.add(
                    PaymentStatusResponse.builder()
                            .success(true)
                            .schemePaymentId(sp.getId())
                            .razorpayPaymentId(sp.getRazorpayPaymentId())
                            .status(sp.getStatus().name())
                            .amount(sp.getAmount())
                            .monthNumber(sp.getMonthNumber())
                            .paymentMethod(
                                    sp.getPaymentMethod() != null ? sp.getPaymentMethod() : "CASH")
                            .paymentDate(
                                    sp.getPaymentDate() != null
                                            ? sp.getPaymentDate().toString()
                                            : null)
                            .paidVia(sp.getRazorpayPaymentId() != null ? "online" : "store")
                            .build());
        }

        // Add PENDING entries for unpaid months
        Set<Integer> paidMonths =
                schemePayments.stream()
                        .filter(p -> p.getStatus() == SchemePayment.PaymentStatus.PAID)
                        .map(SchemePayment::getMonthNumber)
                        .collect(Collectors.toSet());

        for (int m = 1; m <= scheme.getDurationMonths(); m++) {
            if (!paidMonths.contains(m)) {
                history.add(
                        PaymentStatusResponse.builder()
                                .success(false)
                                .status("PENDING")
                                .amount(scheme.getMonthlyAmount())
                                .monthNumber(m)
                                .build());
            }
        }

        // Sort by month number
        history.sort(Comparator.comparingInt(PaymentStatusResponse::getMonthNumber));
        return history;
    }

    // ════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════

    /**
     * Marks a Razorpay payment as successful: updates the RazorpayPayment record, creates a
     * SchemePayment, and links them together.
     */
    private PaymentStatusResponse markPaymentSuccess(
            RazorpayPayment rpPayment,
            String razorpayPaymentId,
            String razorpaySignature,
            String paymentMethod) {

        // Update Razorpay payment record
        rpPayment.setRazorpayPaymentId(razorpayPaymentId);
        rpPayment.setRazorpaySignature(razorpaySignature);
        rpPayment.setStatus(RazorpayPayment.RazorpayStatus.PAID);
        if (paymentMethod != null) {
            rpPayment.setPaymentMethod(paymentMethod);
        }

        // Create SchemePayment record
        BigDecimal amount =
                BigDecimal.valueOf(rpPayment.getAmountPaise()).divide(BigDecimal.valueOf(100));

        SchemePayment schemePayment =
                SchemePayment.builder()
                        .member(rpPayment.getSchemeMember())
                        .monthNumber(rpPayment.getMonthNumber())
                        .amount(amount)
                        .paymentDate(LocalDate.now())
                        .status(SchemePayment.PaymentStatus.PAID)
                        .razorpayPaymentId(razorpayPaymentId)
                        .paymentMethod(
                                rpPayment.getPaymentMethod() != null
                                        ? rpPayment.getPaymentMethod()
                                        : "ONLINE")
                        .build();
        schemePayment.setActive(true);
        schemePaymentRepository.save(schemePayment);

        // Link scheme payment to razorpay payment
        rpPayment.setSchemePayment(schemePayment);
        razorpayPaymentRepository.save(rpPayment);

        log.info(
                "Payment successful: orderId={}, paymentId={}, month={}, amount={}",
                rpPayment.getRazorpayOrderId(),
                razorpayPaymentId,
                rpPayment.getMonthNumber(),
                amount);

        return PaymentStatusResponse.builder()
                .success(true)
                .message("Payment verified successfully")
                .schemePaymentId(schemePayment.getId())
                .razorpayPaymentId(razorpayPaymentId)
                .status("PAID")
                .amount(amount)
                .monthNumber(rpPayment.getMonthNumber())
                .paymentMethod(rpPayment.getPaymentMethod())
                .build();
    }

    /** Builds the order response DTO from a RazorpayPayment record. */
    private CreatePaymentOrderResponse buildOrderResponse(
            RazorpayPayment rpPayment, Scheme scheme, Customer customer) {
        String description =
                scheme.getName() + " - Month " + rpPayment.getMonthNumber() + " Payment";

        return CreatePaymentOrderResponse.builder()
                .orderId(rpPayment.getRazorpayOrderId())
                .amount(rpPayment.getAmountPaise())
                .currency("INR")
                .razorpayKeyId(razorpayKeyId)
                .customerName(
                        customer.getFirstName()
                                + (customer.getLastName() != null
                                        ? " " + customer.getLastName()
                                        : ""))
                .customerPhone(customer.getPhone())
                .customerEmail(customer.getEmail())
                .schemeName(scheme.getName())
                .monthNumber(rpPayment.getMonthNumber())
                .description(description)
                .build();
    }
}
