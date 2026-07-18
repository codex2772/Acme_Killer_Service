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

import com.aurajewels.jewel.entity.CustomOrder;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for CustomOrder entities.
 *
 * @author Raviraj Bhosale
 */
@Repository
public interface CustomOrderRepository extends JpaRepository<CustomOrder, Long> {

    List<CustomOrder> findByStoreIdAndActiveTrueOrderByCreatedAtDesc(Long storeId);

    Optional<CustomOrder> findByIdAndStoreId(Long id, Long storeId);

    @Query(
            "SELECT o FROM CustomOrder o WHERE o.store.id = :storeId AND o.active = true "
                    + "AND (:status IS NULL OR o.status = :status) "
                    + "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) "
                    + "AND (:from IS NULL OR o.orderDate >= :from) "
                    + "AND (:to IS NULL OR o.orderDate <= :to) "
                    + "ORDER BY o.createdAt DESC")
    List<CustomOrder> findFiltered(
            Long storeId,
            CustomOrder.OrderStatus status,
            CustomOrder.PaymentStatus paymentStatus,
            LocalDate from,
            LocalDate to);

    @Query(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(o.orderNumber, LENGTH(:prefix) + 1) AS int)), 0) "
                    + "FROM CustomOrder o WHERE o.store.id = :storeId AND o.orderNumber LIKE CONCAT(:prefix, '%')")
    int findMaxOrderNumber(Long storeId, String prefix);
}
