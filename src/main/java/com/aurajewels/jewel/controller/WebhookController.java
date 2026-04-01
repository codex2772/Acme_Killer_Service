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
package com.aurajewels.jewel.controller;

import com.aurajewels.jewel.service.RazorpayPaymentService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling external webhook callbacks. Currently supports Razorpay payment
 * webhooks. These endpoints are unauthenticated (no JWT) — security is via signature verification.
 *
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final RazorpayPaymentService razorpayPaymentService;

    /**
     * POST /api/webhooks/razorpay — Razorpay server-to-server webhook. Receives payment events
     * (payment.captured, payment.failed). Always returns 200 OK to prevent Razorpay from retrying.
     */
    @PostMapping("/razorpay")
    public ResponseEntity<Map<String, String>> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Razorpay webhook received");

        if (signature == null || signature.isBlank()) {
            log.warn("Razorpay webhook missing signature header");
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "missing signature"));
        }

        try {
            razorpayPaymentService.handleWebhook(payload, signature);
        } catch (Exception e) {
            // Always return 200 to Razorpay — log the error for debugging
            log.error("Error processing Razorpay webhook: {}", e.getMessage(), e);
        }

        return ResponseEntity.ok(Map.of("status", "processed"));
    }
}
