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

import com.aurajewels.jewel.dto.accounts.ExpenseRequest;
import com.aurajewels.jewel.entity.Expense;
import com.aurajewels.jewel.entity.LedgerEntry;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.ExpenseRepository;
import com.aurajewels.jewel.repository.LedgerEntryRepository;
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
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final StoreRepository storeRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<Expense> listExpenses(String category, LocalDate from, LocalDate to) {
        Long storeId = StoreContext.getCurrentStoreId();
        return expenseRepository.findFiltered(storeId, category, from, to);
    }

    @Transactional(readOnly = true)
    public Expense getExpense(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return expenseRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
    }

    @Transactional
    public Expense createExpense(ExpenseRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Expense expense =
                Expense.builder()
                        .store(store)
                        .expenseDate(
                                request.getDate() != null ? request.getDate() : LocalDate.now())
                        .category(request.getCategory())
                        .description(request.getDescription())
                        .amount(request.getAmount())
                        .mode(request.getMode())
                        .createdBy(StoreContext.getCurrentUserId())
                        .active(true)
                        .build();

        expense = expenseRepository.save(expense);

        // Auto-create DR ledger entry for expense
        LedgerEntry ledgerEntry =
                LedgerEntry.builder()
                        .store(store)
                        .entryDate(expense.getExpenseDate())
                        .party(request.getCategory())
                        .type(LedgerEntry.LedgerType.DR)
                        .amount(request.getAmount())
                        .mode(request.getMode() != null ? request.getMode() : "CASH")
                        .note("Expense — " + request.getDescription())
                        .category("Expense")
                        .referenceId(String.valueOf(expense.getId()))
                        .referenceType("EXPENSE")
                        .createdBy(StoreContext.getCurrentUserId())
                        .active(true)
                        .build();
        ledgerEntryRepository.save(ledgerEntry);

        activityLogService.log(
                "Created Expense",
                request.getCategory() + " — ₹" + request.getAmount(),
                "Accounts",
                "EXPENSE",
                expense.getId());

        return expense;
    }

    @Transactional
    public Expense updateExpense(Long id, ExpenseRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Expense expense =
                expenseRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (request.getDate() != null) expense.setExpenseDate(request.getDate());
        if (request.getCategory() != null) expense.setCategory(request.getCategory());
        if (request.getDescription() != null) expense.setDescription(request.getDescription());
        if (request.getAmount() != null) expense.setAmount(request.getAmount());
        if (request.getMode() != null) expense.setMode(request.getMode());

        return expenseRepository.save(expense);
    }

    @Transactional
    public void deleteExpense(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        Expense expense =
                expenseRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        expense.setActive(false);
        expenseRepository.save(expense);
    }
}
