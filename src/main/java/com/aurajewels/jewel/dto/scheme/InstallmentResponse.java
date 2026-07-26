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
package com.aurajewels.jewel.dto.scheme;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of a scheme member's installment schedule.
 *
 * <p>Field names match the desktop client contract: {@code month}, {@code amount}, {@code date},
 * {@code status} where status ∈ {PAID, DUE, UPCOMING}.
 *
 * @author Raviraj Bhosale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentResponse {

    /** 1-based installment index (1..durationMonths). */
    private Integer month;

    /** Installment amount (paid amount for PAID rows, monthly amount otherwise). */
    private BigDecimal amount;

    /** Payment date for PAID rows; due date for DUE/UPCOMING rows. */
    private LocalDate date;

    /** PAID, DUE, or UPCOMING. */
    private String status;
}
