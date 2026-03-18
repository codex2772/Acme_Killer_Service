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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * @author Raviraj Bhosale
 */
@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @JsonIgnore
    private Store store;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "gst", length = 20)
    private String gst;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @Column(name = "active")
    private Boolean active = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "supplier_metals", joinColumns = @JoinColumn(name = "supplier_id"))
    @Column(name = "metal")
    @Builder.Default
    private List<String> metals = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum SupplierStatus {
        ACTIVE,
        INACTIVE
    }
}
