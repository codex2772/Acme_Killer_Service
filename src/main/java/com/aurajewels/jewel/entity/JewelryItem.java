/*
 * MIT License
 *
 * Copyright (c) 2026 AuraJewels
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
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "jewelry_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JewelryItem extends BaseEntity {

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metal_type_id", nullable = false)
    private MetalType metalType;

    @Column(name = "gross_weight", nullable = false, precision = 10, scale = 3)
    private BigDecimal grossWeight;

    @Column(name = "net_weight", nullable = false, precision = 10, scale = 3)
    private BigDecimal netWeight;

    @Column(name = "making_charges", nullable = false, precision = 12, scale = 2)
    private BigDecimal makingCharges = BigDecimal.ZERO;

    @Column(name = "stone_charges", nullable = false, precision = 12, scale = 2)
    private BigDecimal stoneCharges = BigDecimal.ZERO;

    @Column(name = "other_charges", nullable = false, precision = 12, scale = 2)
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "barcode", length = 100)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ItemStatus status = ItemStatus.IN_STOCK;

    public enum ItemStatus {
        IN_STOCK,
        SOLD,
        ON_APPROVAL,
        RETURNED,
        DAMAGED
    }
}
