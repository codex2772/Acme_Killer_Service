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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * @author Raviraj Bhosale
 */
@Entity
@Table(name = "daily_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnore
    private Store store;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Column(name = "gold_24k", precision = 12, scale = 2)
    private BigDecimal gold24k = BigDecimal.ZERO;

    @Column(name = "gold_22k", precision = 12, scale = 2)
    private BigDecimal gold22k = BigDecimal.ZERO;

    @Column(name = "gold_18k", precision = 12, scale = 2)
    private BigDecimal gold18k = BigDecimal.ZERO;

    @Column(name = "gold_14k", precision = 12, scale = 2)
    private BigDecimal gold14k = BigDecimal.ZERO;

    @Column(name = "silver", precision = 12, scale = 2)
    private BigDecimal silver = BigDecimal.ZERO;

    @Column(name = "platinum", precision = 12, scale = 2)
    private BigDecimal platinum = BigDecimal.ZERO;

    @Column(name = "rhodium", precision = 12, scale = 2)
    private BigDecimal rhodium = BigDecimal.ZERO;

    @Column(name = "rose_gold_18k", precision = 12, scale = 2)
    private BigDecimal roseGold18k = BigDecimal.ZERO;

    @Column(name = "white_gold_18k", precision = 12, scale = 2)
    private BigDecimal whiteGold18k = BigDecimal.ZERO;

    @Column(name = "updated_by", length = 150)
    private String updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
