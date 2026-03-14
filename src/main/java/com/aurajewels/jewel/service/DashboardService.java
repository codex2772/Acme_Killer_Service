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
package com.aurajewels.jewel.service;

import com.aurajewels.jewel.dto.dashboard.DashboardSummary;
import com.aurajewels.jewel.dto.dashboard.RevenuePoint;
import com.aurajewels.jewel.entity.Invoice;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.InvoiceRepository;
import com.aurajewels.jewel.repository.JewelryItemRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final JewelryItemRepository jewelryItemRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public DashboardSummary getSummary() {
        Long storeId = StoreContext.getCurrentStoreId();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        BigDecimal todaySales = invoiceRepository.sumSalesByDate(storeId, today);
        BigDecimal monthSales = invoiceRepository.sumSalesByDateRange(storeId, monthStart, today);

        long totalInventory = jewelryItemRepository.findByStoreIdAndActiveTrue(storeId).size();
        long pendingInvoices =
                invoiceRepository.countByStoreIdAndPaymentStatusAndActiveTrue(
                        storeId, Invoice.PaymentStatus.UNPAID);
        long totalCustomers = customerRepository.findByStoreIdAndActiveTrue(storeId).size();

        // Revenue for each day this month
        List<RevenuePoint> revenueThisMonth = new ArrayList<>();
        for (LocalDate d = monthStart; !d.isAfter(today); d = d.plusDays(1)) {
            BigDecimal amount = invoiceRepository.sumSalesByDate(storeId, d);
            revenueThisMonth.add(new RevenuePoint(d, amount));
        }

        return DashboardSummary.builder()
                .todaySales(todaySales)
                .monthSales(monthSales)
                .totalInventory(totalInventory)
                .inventoryValue(BigDecimal.ZERO) // To be computed with item prices
                .pendingInvoices(pendingInvoices)
                .totalCustomers(totalCustomers)
                .todayTransactions(0) // Can be computed from ledger entries
                .topSellingCategory(null)
                .revenueThisMonth(revenueThisMonth)
                .build();
    }
}
