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

import com.aurajewels.jewel.dto.accounts.CloseRegisterRequest;
import com.aurajewels.jewel.dto.accounts.OpenRegisterRequest;
import com.aurajewels.jewel.entity.CashRegister;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.CashRegisterRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.math.BigDecimal;
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
public class CashRegisterService {

    private final CashRegisterRepository cashRegisterRepository;
    private final StoreRepository storeRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<CashRegister> listRegisters() {
        Long storeId = StoreContext.getCurrentStoreId();
        return cashRegisterRepository.findByStoreIdOrderByRegisterDateDesc(storeId);
    }

    @Transactional(readOnly = true)
    public CashRegister getCurrentRegister() {
        Long storeId = StoreContext.getCurrentStoreId();
        return cashRegisterRepository
                .findByStoreIdAndStatus(storeId, CashRegister.RegisterStatus.OPEN)
                .orElse(null);
    }

    @Transactional
    public CashRegister openRegister(OpenRegisterRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        // Check if there's already an open register
        cashRegisterRepository
                .findByStoreIdAndStatus(storeId, CashRegister.RegisterStatus.OPEN)
                .ifPresent(
                        r -> {
                            throw new IllegalArgumentException(
                                    "A register is already open for today. Close it first.");
                        });

        CashRegister register =
                CashRegister.builder()
                        .store(store)
                        .registerDate(LocalDate.now())
                        .openingBalance(
                                request.getOpeningBalance() != null
                                        ? request.getOpeningBalance()
                                        : BigDecimal.ZERO)
                        .cashIn(BigDecimal.ZERO)
                        .cashOut(BigDecimal.ZERO)
                        .openedBy(StoreContext.getCurrentUserId())
                        .status(CashRegister.RegisterStatus.OPEN)
                        .build();

        register = cashRegisterRepository.save(register);

        activityLogService.log(
                "Opened Cash Register",
                "Opening balance: ₹" + register.getOpeningBalance(),
                "Accounts",
                "CASH_REGISTER",
                register.getId());

        return register;
    }

    @Transactional
    public CashRegister closeRegister(Long id, CloseRegisterRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        CashRegister register =
                cashRegisterRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Cash register not found"));

        if (register.getStatus() == CashRegister.RegisterStatus.CLOSED) {
            throw new IllegalArgumentException("Register is already closed");
        }

        register.setClosingBalance(request.getClosingBalance());
        register.setClosedBy(StoreContext.getCurrentUserId());
        register.setStatus(CashRegister.RegisterStatus.CLOSED);

        register = cashRegisterRepository.save(register);

        activityLogService.log(
                "Closed Cash Register",
                "Closing balance: ₹" + register.getClosingBalance(),
                "Accounts",
                "CASH_REGISTER",
                register.getId());

        return register;
    }
}
