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
package com.aurajewels.jewel.dto.customorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/**
 * DTO for creating or updating a bespoke custom order.
 *
 * @author Raviraj Bhosale
 */
@Data
public class CustomOrderRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    private LocalDate date;

    @Valid private List<CustomOrderItemRequest> items;

    // subtotal, gstAmount and total are recomputed server-side from the items and are ignored here.
    private BigDecimal subtotal;

    @PositiveOrZero(message = "gstRate cannot be negative")
    private BigDecimal gstRate;

    private BigDecimal gstAmount;

    @PositiveOrZero(message = "discount cannot be negative")
    private BigDecimal discount;

    private BigDecimal roundOff;
    private BigDecimal total;
    private LocalDate expectedDeliveryDate;
    private String goldsmithName;
    private Long assignedTo;
    private String designNotes;
    private String referenceImageUrl;
    private String notes;
    private String digitalSignature;
    private String status;
}
