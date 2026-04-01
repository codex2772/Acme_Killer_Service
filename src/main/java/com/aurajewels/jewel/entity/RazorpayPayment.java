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
package com.aurajewels.jewel.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity tracking Razorpay payment orders and their status. Links to SchemePayment once payment
 * is successfully verified.
 *
 * @author Raviraj Bhosale
 */
@Entity
@Table(name = "razorpay_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayPayment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_payment_id")
    private SchemePayment schemePayment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_member_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private SchemeMember schemeMember;

    @Column(name = "razorpay_order_id", nullable = false, unique = true, length = 50)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 50)
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature", length = 255)
    private String razorpaySignature;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(name = "currency", length = 5)
    private String currency;

    @Column(name = "month_number", nullable = false)
    private Integer monthNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private RazorpayStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    public enum RazorpayStatus {
        CREATED,
        AUTHORIZED,
        CAPTURED,
        PAID,
        FAILED,
        REFUNDED
    }
}
