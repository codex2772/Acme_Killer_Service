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
package com.aurajewels.jewel.service;

import com.aurajewels.jewel.dto.accounts.LedgerEntryRequest;
import com.aurajewels.jewel.entity.LedgerEntry;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.LedgerEntryRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final StoreRepository storeRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<LedgerEntry> listEntries(String type, LocalDate from, LocalDate to) {
        Long storeId = StoreContext.getCurrentStoreId();
        LedgerEntry.LedgerType ledgerType =
                type != null ? LedgerEntry.LedgerType.valueOf(type) : null;
        return ledgerEntryRepository.findFiltered(storeId, ledgerType, from, to);
    }

    @Transactional(readOnly = true)
    public LedgerEntry getEntry(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return ledgerEntryRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Ledger entry not found"));
    }

    @Transactional
    public LedgerEntry createEntry(LedgerEntryRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        LedgerEntry entry =
                LedgerEntry.builder()
                        .store(store)
                        .entryDate(request.getDate() != null ? request.getDate() : LocalDate.now())
                        .party(request.getParty())
                        .type(LedgerEntry.LedgerType.valueOf(request.getType()))
                        .amount(request.getAmount())
                        .mode(request.getMode())
                        .note(request.getNote())
                        .category(request.getCategory())
                        .referenceId(request.getReferenceId())
                        .referenceType(request.getReferenceType())
                        .createdBy(StoreContext.getCurrentUserId())
                        .active(true)
                        .build();

        entry = ledgerEntryRepository.save(entry);

        activityLogService.log(
                "Created Ledger Entry",
                request.getType() + " ₹" + request.getAmount() + " — " + request.getParty(),
                "Accounts",
                "LEDGER",
                entry.getId());

        return entry;
    }
}
