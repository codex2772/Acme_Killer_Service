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

import com.aurajewels.jewel.dto.rates.RateAlertRequest;
import com.aurajewels.jewel.dto.rates.RateRequest;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.DailyRate;
import com.aurajewels.jewel.entity.RateAlert;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.DailyRateRepository;
import com.aurajewels.jewel.repository.RateAlertRepository;
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
public class DailyRateService {

    private final DailyRateRepository dailyRateRepository;
    private final RateAlertRepository rateAlertRepository;
    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public DailyRate getCurrentRates() {
        Long storeId = StoreContext.getCurrentStoreId();
        return dailyRateRepository.findFirstByStoreIdOrderByRateDateDesc(storeId).orElse(null);
    }

    @Transactional
    public DailyRate updateRates(RateRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        LocalDate today = LocalDate.now();
        DailyRate rate =
                dailyRateRepository
                        .findByStoreIdAndRateDate(storeId, today)
                        .orElseGet(() -> DailyRate.builder().store(store).rateDate(today).build());

        if (request.getGold24k() != null) rate.setGold24k(request.getGold24k());
        if (request.getGold22k() != null) rate.setGold22k(request.getGold22k());
        if (request.getGold18k() != null) rate.setGold18k(request.getGold18k());
        if (request.getGold14k() != null) rate.setGold14k(request.getGold14k());
        if (request.getSilver() != null) rate.setSilver(request.getSilver());
        if (request.getPlatinum() != null) rate.setPlatinum(request.getPlatinum());
        if (request.getRhodium() != null) rate.setRhodium(request.getRhodium());
        if (request.getRoseGold18k() != null) rate.setRoseGold18k(request.getRoseGold18k());
        if (request.getWhiteGold18k() != null) rate.setWhiteGold18k(request.getWhiteGold18k());
        rate.setUpdatedBy(
                StoreContext.getCurrentUserId() != null
                        ? StoreContext.getCurrentUserId().toString()
                        : "System");

        rate = dailyRateRepository.save(rate);

        activityLogService.log(
                "Updated Rates",
                "Gold 22K: ₹" + rate.getGold22k() + ", Silver: ₹" + rate.getSilver(),
                "Rates",
                "DAILY_RATE",
                rate.getId());

        return rate;
    }

    @Transactional(readOnly = true)
    public List<DailyRate> getRateHistory(int days) {
        Long storeId = StoreContext.getCurrentStoreId();
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days);
        return dailyRateRepository.findByStoreIdAndRateDateBetweenOrderByRateDateDesc(
                storeId, from, to);
    }

    @Transactional
    public RateAlert createAlert(RateAlertRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        RateAlert.RateAlertBuilder alertBuilder =
                RateAlert.builder()
                        .store(store)
                        .metal(request.getMetal())
                        .conditionType(RateAlert.ConditionType.valueOf(request.getCondition()))
                        .threshold(request.getThreshold())
                        .isActive(true);

        if (request.getCustomerId() != null) {
            Customer customer =
                    customerRepository
                            .findById(request.getCustomerId())
                            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            alertBuilder.customer(customer);
        }

        return rateAlertRepository.save(alertBuilder.build());
    }

    @Transactional(readOnly = true)
    public List<RateAlert> getActiveAlerts() {
        Long storeId = StoreContext.getCurrentStoreId();
        return rateAlertRepository.findByStoreIdAndIsActiveTrue(storeId);
    }
}
