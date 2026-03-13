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
package com.aurajewels.jewel.repository;

import com.aurajewels.jewel.entity.Invoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByStoreIdAndActiveTrueOrderByCreatedAtDesc(Long storeId);

    Optional<Invoice> findByIdAndStoreId(Long id, Long storeId);

    @Query(
            "SELECT i FROM Invoice i WHERE i.store.id = :storeId AND i.active = true "
                    + "AND (:status IS NULL OR i.status = :status) "
                    + "AND (:paymentStatus IS NULL OR i.paymentStatus = :paymentStatus) "
                    + "AND (:from IS NULL OR i.invoiceDate >= :from) "
                    + "AND (:to IS NULL OR i.invoiceDate <= :to) "
                    + "ORDER BY i.createdAt DESC")
    List<Invoice> findFiltered(
            Long storeId,
            Invoice.InvoiceStatus status,
            Invoice.PaymentStatus paymentStatus,
            LocalDate from,
            LocalDate to);

    @Query(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(i.invoiceNumber, LENGTH(:prefix) + 1) AS int)), 0) "
                    + "FROM Invoice i WHERE i.store.id = :storeId AND i.invoiceNumber LIKE CONCAT(:prefix, '%')")
    int findMaxInvoiceNumber(Long storeId, String prefix);

    @Query(
            "SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.store.id = :storeId "
                    + "AND i.status = 'CONFIRMED' AND i.invoiceDate = :date")
    java.math.BigDecimal sumSalesByDate(Long storeId, LocalDate date);

    @Query(
            "SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.store.id = :storeId "
                    + "AND i.status = 'CONFIRMED' AND i.invoiceDate BETWEEN :from AND :to")
    java.math.BigDecimal sumSalesByDateRange(Long storeId, LocalDate from, LocalDate to);

    long countByStoreIdAndPaymentStatusAndActiveTrue(
            Long storeId, Invoice.PaymentStatus paymentStatus);
}
