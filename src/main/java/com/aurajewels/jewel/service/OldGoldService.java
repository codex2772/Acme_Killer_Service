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

import com.aurajewels.jewel.dto.oldgold.MeltingRecordRequest;
import com.aurajewels.jewel.dto.oldgold.OldGoldRequest;
import com.aurajewels.jewel.dto.oldgold.PurityTestRequest;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.LedgerEntryRepository;
import com.aurajewels.jewel.repository.OldGoldPurchaseRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class OldGoldService {

    private final OldGoldPurchaseRepository oldGoldPurchaseRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<OldGoldPurchase> listPurchases() {
        Long storeId = StoreContext.getCurrentStoreId();
        return oldGoldPurchaseRepository.findByStoreIdAndActiveTrueOrderByCreatedAtDesc(storeId);
    }

    @Transactional(readOnly = true)
    public OldGoldPurchase getPurchase(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return oldGoldPurchaseRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Old gold purchase not found"));
    }

    @Transactional
    public OldGoldPurchase createPurchase(OldGoldRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Customer customer =
                customerRepository
                        .findById(request.getCustomerId())
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        OldGoldPurchase purchase =
                OldGoldPurchase.builder()
                        .store(store)
                        .customer(customer)
                        .purchaseDate(
                                request.getDate() != null ? request.getDate() : LocalDate.now())
                        .weight(request.getWeight())
                        .purity(request.getPurity())
                        .rate(request.getRate())
                        .total(request.getTotal())
                        .type(
                                request.getType() != null
                                        ? OldGoldPurchase.OldGoldType.valueOf(request.getType())
                                        : OldGoldPurchase.OldGoldType.PURCHASE)
                        .kycDone(request.getKycDone() != null ? request.getKycDone() : false)
                        .notes(request.getNotes())
                        .createdBy(StoreContext.getCurrentUserId())
                        .active(true)
                        .build();

        // Add purity test if provided
        if (request.getPurityTest() != null) {
            PurityTestRequest pt = request.getPurityTest();
            OldGoldPurityTest test =
                    OldGoldPurityTest.builder()
                            .oldGoldPurchase(purchase)
                            .method(pt.getMethod())
                            .actualPurity(pt.getActualPurity())
                            .purityPercent(pt.getPurityPercent())
                            .testedBy(pt.getTestedBy())
                            .build();
            purchase.getPurityTests().add(test);
        }

        // Add melting record if provided
        if (request.getMeltingRecord() != null) {
            MeltingRecordRequest mr = request.getMeltingRecord();
            OldGoldMeltingRecord record =
                    OldGoldMeltingRecord.builder()
                            .oldGoldPurchase(purchase)
                            .meltedWeight(mr.getMeltedWeight())
                            .meltDate(mr.getMeltDate() != null ? mr.getMeltDate() : LocalDate.now())
                            .meltedBy(mr.getMeltedBy())
                            .build();
            purchase.getMeltingRecords().add(record);
        }

        purchase = oldGoldPurchaseRepository.save(purchase);

        // Auto-create DR ledger entry for old gold purchase
        String customerName =
                customer.getFirstName()
                        + (customer.getLastName() != null ? " " + customer.getLastName() : "");
        LedgerEntry ledgerEntry =
                LedgerEntry.builder()
                        .store(store)
                        .entryDate(purchase.getPurchaseDate())
                        .party(customerName)
                        .type(LedgerEntry.LedgerType.DR)
                        .amount(purchase.getTotal())
                        .mode("CASH")
                        .note(
                                "Old Gold Purchase — "
                                        + purchase.getWeight()
                                        + "g "
                                        + purchase.getPurity())
                        .category("Old Gold")
                        .referenceId(String.valueOf(purchase.getId()))
                        .referenceType("OLD_GOLD")
                        .createdBy(StoreContext.getCurrentUserId())
                        .active(true)
                        .build();
        ledgerEntryRepository.save(ledgerEntry);

        activityLogService.log(
                "Created Old Gold Purchase",
                request.getWeight() + "g " + request.getPurity() + " — ₹" + request.getTotal(),
                "OldGold",
                "OLD_GOLD",
                purchase.getId());

        return purchase;
    }

    @Transactional
    public OldGoldPurchase updatePurchase(Long id, OldGoldRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        OldGoldPurchase purchase =
                oldGoldPurchaseRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Old gold purchase not found"));

        if (request.getWeight() != null) purchase.setWeight(request.getWeight());
        if (request.getPurity() != null) purchase.setPurity(request.getPurity());
        if (request.getRate() != null) purchase.setRate(request.getRate());
        if (request.getTotal() != null) purchase.setTotal(request.getTotal());
        if (request.getKycDone() != null) purchase.setKycDone(request.getKycDone());
        if (request.getNotes() != null) purchase.setNotes(request.getNotes());

        // Add new purity test if provided
        if (request.getPurityTest() != null) {
            PurityTestRequest pt = request.getPurityTest();
            OldGoldPurityTest test =
                    OldGoldPurityTest.builder()
                            .oldGoldPurchase(purchase)
                            .method(pt.getMethod())
                            .actualPurity(pt.getActualPurity())
                            .purityPercent(pt.getPurityPercent())
                            .testedBy(pt.getTestedBy())
                            .build();
            purchase.getPurityTests().add(test);
        }

        // Add new melting record if provided
        if (request.getMeltingRecord() != null) {
            MeltingRecordRequest mr = request.getMeltingRecord();
            OldGoldMeltingRecord record =
                    OldGoldMeltingRecord.builder()
                            .oldGoldPurchase(purchase)
                            .meltedWeight(mr.getMeltedWeight())
                            .meltDate(mr.getMeltDate() != null ? mr.getMeltDate() : LocalDate.now())
                            .meltedBy(mr.getMeltedBy())
                            .build();
            purchase.getMeltingRecords().add(record);
        }

        return oldGoldPurchaseRepository.save(purchase);
    }
}
