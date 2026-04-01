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
package com.aurajewels.jewel.dto.customerapp;

import lombok.Builder;
import lombok.Data;

/**
 * Response DTO returned to the Flutter app after creating a Razorpay order. Contains all details
 * needed by the Razorpay Flutter SDK to open checkout.
 *
 * @author Raviraj Bhosale
 */
@Data
@Builder
public class CreatePaymentOrderResponse {

    /** Razorpay order ID — pass to Flutter SDK. */
    private String orderId;

    /** Amount in paise (e.g., 500000 = ₹5,000). */
    private Long amount;

    /** Currency code, always "INR". */
    private String currency;

    /** Razorpay public key — pass to Flutter SDK. */
    private String razorpayKeyId;

    /** Customer name — pre-fill in checkout. */
    private String customerName;

    /** Customer phone — pre-fill in checkout. */
    private String customerPhone;

    /** Customer email — pre-fill in checkout (nullable). */
    private String customerEmail;

    /** Scheme name — display in checkout description. */
    private String schemeName;

    /** Which month number this payment is for. */
    private Integer monthNumber;

    /** Human-readable description for checkout. */
    private String description;
}
