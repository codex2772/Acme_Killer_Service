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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Data;

/**
 * DTO for a single custom order line item.
 *
 * @author Raviraj Bhosale
 */
@Data
public class CustomOrderItemRequest {
    private Long jewelryItemId;

    @NotBlank(message = "Item name is required")
    private String name;

    @PositiveOrZero(message = "weight cannot be negative")
    private BigDecimal weight;

    private String purity;

    @PositiveOrZero(message = "rate cannot be negative")
    private BigDecimal rate;

    @PositiveOrZero(message = "makingCharge cannot be negative")
    private BigDecimal makingCharge;

    private String makingChargeType;

    @PositiveOrZero(message = "wastage cannot be negative")
    private BigDecimal wastage;

    @PositiveOrZero(message = "stoneCharges cannot be negative")
    private BigDecimal stoneCharges;

    @PositiveOrZero(message = "amount cannot be negative")
    private BigDecimal amount;
}
