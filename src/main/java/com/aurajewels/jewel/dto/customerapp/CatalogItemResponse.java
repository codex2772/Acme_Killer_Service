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

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/**
 * @author Raviraj Bhosale
 */
@Data
@Builder
public class CatalogItemResponse {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private String categoryName;
    private String metalName;
    private String purity;
    private BigDecimal grossWeight;
    private BigDecimal netWeight;
    private BigDecimal makingCharges;
    private BigDecimal stoneCharges;
    private BigDecimal metalRate;
    private String hsnCode;
    private String status;
    private boolean wishlisted;
    private Instant createdAt;
}
