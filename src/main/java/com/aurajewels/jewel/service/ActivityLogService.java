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

import com.aurajewels.jewel.entity.ActivityLog;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.ActivityLogRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final StoreRepository storeRepository;

    @Async
    @Transactional
    public void log(String action, String detail, String module, String entityType, Long entityId) {
        Long storeId = StoreContext.getCurrentStoreId();
        Long userId = StoreContext.getCurrentUserId();
        if (storeId == null) return;

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) return;

        ActivityLog log =
                ActivityLog.builder()
                        .store(store)
                        .userId(userId)
                        .userName(null)
                        .action(action)
                        .detail(detail)
                        .module(module)
                        .entityType(entityType)
                        .entityId(entityId)
                        .build();
        activityLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> findFiltered(String module, LocalDate from, LocalDate to) {
        Long storeId = StoreContext.getCurrentStoreId();
        Instant fromInstant =
                from != null ? from.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        Instant toInstant =
                to != null ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
        return activityLogRepository.findFiltered(storeId, module, fromInstant, toInstant);
    }
}
