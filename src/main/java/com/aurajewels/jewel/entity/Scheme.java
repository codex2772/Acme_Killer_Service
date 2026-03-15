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
 * @author Diksha Mohite
 */
@Entity
@Table(name = "schemes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scheme extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Store store;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "monthly_amount", nullable = false)
    private java.math.BigDecimal monthlyAmount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SchemeStatus status;

    @Column(name = "start_date", nullable = false)
    private java.time.LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private java.time.LocalDate endDate;

    @Column(name = "bonus_month")
    private Boolean bonusMonth;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column private Boolean active;

    public enum SchemeStatus {
        ACTIVE,
        CLOSED,
        PAUSED
    }
}
