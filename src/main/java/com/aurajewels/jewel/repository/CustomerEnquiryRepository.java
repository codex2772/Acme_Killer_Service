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
package com.aurajewels.jewel.repository;

import com.aurajewels.jewel.entity.CustomerEnquiry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Raviraj Bhosale
 */
@Repository
public interface CustomerEnquiryRepository extends JpaRepository<CustomerEnquiry, Long> {

    List<CustomerEnquiry> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<CustomerEnquiry> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    /** Admin-side: fetch enquiries with customer and item eagerly loaded. */
    @Query(
            "SELECT e FROM CustomerEnquiry e"
                    + " JOIN FETCH e.customer"
                    + " LEFT JOIN FETCH e.jewelryItem"
                    + " WHERE e.store.id = :storeId"
                    + " ORDER BY e.createdAt DESC")
    List<CustomerEnquiry> findByStoreIdWithDetails(@Param("storeId") Long storeId);

    /** Admin-side: fetch single enquiry with customer and item eagerly loaded. */
    @Query(
            "SELECT e FROM CustomerEnquiry e"
                    + " JOIN FETCH e.customer"
                    + " LEFT JOIN FETCH e.jewelryItem"
                    + " WHERE e.id = :id")
    Optional<CustomerEnquiry> findByIdWithDetails(@Param("id") Long id);
}
