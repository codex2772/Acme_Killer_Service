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
package com.aurajewels.jewel.dto.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author Raviraj Bhosale
 */
@Data
@Builder
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private Long customerId;
    private String customer;
    private Long storeId;
    private String type;
    private LocalDate date;
    private List<InvoiceItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal gstRate;
    private BigDecimal gstAmount;
    private BigDecimal discount;
    private BigDecimal roundOff;
    private BigDecimal total;
    private BigDecimal oldGoldAdjustment;
    private String paymentMode;
    private List<PaymentResponse> splitPayments;
    private LocalDate dueDate;
    private String status;
    private String paymentStatus;
    private BigDecimal paidAmount;
    private String notes;
    private String digitalSignature;
    private Instant createdAt;
}
