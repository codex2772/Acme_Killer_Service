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

import com.aurajewels.jewel.dto.billing.*;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.*;
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
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<CreditNote> listCreditNotes() {
        Long storeId = StoreContext.getCurrentStoreId();
        return creditNoteRepository.findByStoreIdAndActiveTrueOrderByCreatedAtDesc(storeId);
    }

    @Transactional(readOnly = true)
    public CreditNote getCreditNote(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return creditNoteRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Credit Note not found"));
    }

    @Transactional
    public CreditNote createCreditNote(CreditNoteRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Customer customer =
                customerRepository
                        .findById(request.getCustomerId())
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        String prefix = "CN-";
        int nextNum = creditNoteRepository.findMaxCreditNoteNumber(storeId, prefix) + 1;
        String cnNumber = prefix + String.format("%05d", nextNum);

        CreditNote creditNote =
                CreditNote.builder()
                        .store(store)
                        .creditNoteNumber(cnNumber)
                        .customer(customer)
                        .originalInvoiceId(request.getOriginalInvoiceId())
                        .creditNoteDate(
                                request.getDate() != null ? request.getDate() : LocalDate.now())
                        .subtotal(
                                request.getSubtotal() != null
                                        ? request.getSubtotal()
                                        : BigDecimal.ZERO)
                        .discount(
                                request.getDiscount() != null
                                        ? request.getDiscount()
                                        : BigDecimal.ZERO)
                        .gstRate(
                                request.getGstRate() != null
                                        ? request.getGstRate()
                                        : new BigDecimal("3.00"))
                        .gstAmount(
                                request.getGstAmount() != null
                                        ? request.getGstAmount()
                                        : BigDecimal.ZERO)
                        .roundOff(
                                request.getRoundOff() != null
                                        ? request.getRoundOff()
                                        : BigDecimal.ZERO)
                        .totalAmount(
                                request.getTotal() != null ? request.getTotal() : BigDecimal.ZERO)
                        .returnReason(request.getReturnReason())
                        .refundMode(
                                request.getRefundMode() != null
                                        ? CreditNote.RefundMode.valueOf(request.getRefundMode())
                                        : CreditNote.RefundMode.SAME_MODE)
                        .notes(request.getNotes())
                        .status(CreditNote.CreditNoteStatus.DRAFT)
                        .active(true)
                        .createdBy(StoreContext.getCurrentUserId())
                        .items(new ArrayList<>())
                        .build();

        if (request.getItems() != null) {
            for (InvoiceItemRequest itemReq : request.getItems()) {
                CreditNoteItem item =
                        CreditNoteItem.builder()
                                .creditNote(creditNote)
                                .store(store)
                                .jewelryItemId(itemReq.getJewelryItemId())
                                .name(itemReq.getName())
                                .weight(itemReq.getWeight())
                                .purity(itemReq.getPurity())
                                .rate(itemReq.getRate())
                                .makingCharge(itemReq.getMakingCharge())
                                .makingChargeType(
                                        itemReq.getMakingChargeType() != null
                                                ? EstimateItem.MakingChargeType.valueOf(
                                                        itemReq.getMakingChargeType())
                                                : EstimateItem.MakingChargeType.PERCENTAGE)
                                .wastage(itemReq.getWastage())
                                .amount(itemReq.getAmount())
                                .build();
                creditNote.getItems().add(item);
            }
        }

        creditNote = creditNoteRepository.save(creditNote);

        // Auto-create DR ledger entry for credit note (refund)
        String customerName =
                customer.getFirstName()
                        + (customer.getLastName() != null ? " " + customer.getLastName() : "");
        LedgerEntry ledgerEntry =
                LedgerEntry.builder()
                        .store(store)
                        .entryDate(creditNote.getCreditNoteDate())
                        .party(customerName)
                        .type(LedgerEntry.LedgerType.DR)
                        .amount(creditNote.getTotalAmount())
                        .mode(request.getRefundMode() != null ? request.getRefundMode() : "CASH")
                        .note("Refund — " + cnNumber)
                        .category("Sales Return")
                        .referenceId(cnNumber)
                        .referenceType("CREDIT_NOTE")
                        .createdBy(StoreContext.getCurrentUserId())
                        .active(true)
                        .build();
        ledgerEntryRepository.save(ledgerEntry);

        activityLogService.log(
                "Created Credit Note",
                "Credit Note " + cnNumber + " for " + customer.getFirstName(),
                "Billing",
                "CREDIT_NOTE",
                creditNote.getId());

        return creditNote;
    }
}
