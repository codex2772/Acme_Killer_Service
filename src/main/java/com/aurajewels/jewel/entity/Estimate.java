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
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * @author Raviraj Bhosale
 */
@Entity
@Table(name = "estimates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estimate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnore
    private Store store;

    @Column(name = "estimate_number", nullable = false)
    private String estimateNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "estimate_date", nullable = false)
    private LocalDate estimateDate;

    @Column(name = "subtotal", precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount", precision = 14, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate = new BigDecimal("3.00");

    @Column(name = "gst_amount", precision = 14, scale = 2)
    private BigDecimal gstAmount = BigDecimal.ZERO;

    @Column(name = "round_off", precision = 10, scale = 2)
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "digital_signature", length = 200)
    private String digitalSignature;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EstimateStatus status = EstimateStatus.DRAFT;

    @Column(name = "converted_invoice_id")
    private Long convertedInvoiceId;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(
            mappedBy = "estimate",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @Builder.Default
    private List<EstimateItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum EstimateStatus {
        DRAFT,
        SENT,
        ACCEPTED,
        EXPIRED,
        CONVERTED
    }
}
