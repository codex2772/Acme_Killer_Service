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
package com.aurajewels.jewel.service.admin;

import com.aurajewels.jewel.dto.admin.PlatformDashboardResponse;
import com.aurajewels.jewel.entity.OrgSubscription;
import com.aurajewels.jewel.repository.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class PlatformDashboardService {

    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final OrgSubscriptionRepository orgSubscriptionRepository;

    @Transactional(readOnly = true)
    public PlatformDashboardResponse getDashboard() {
        long totalOrgs = organizationRepository.count();
        long totalStores = storeRepository.count();
        long totalUsers = userRepository.count();
        long totalCustomers = customerRepository.count();

        // Plan breakdown
        List<OrgSubscription> subs = orgSubscriptionRepository.findAll();
        Map<String, Long> planBreakdown = new HashMap<>();
        long activeOrgs = 0;
        long trialOrgs = 0;
        long suspendedOrgs = 0;

        for (OrgSubscription sub : subs) {
            planBreakdown.merge(sub.getPlan().name(), 1L, Long::sum);
            switch (sub.getStatus()) {
                case ACTIVE -> activeOrgs++;
                case TRIAL -> trialOrgs++;
                case SUSPENDED -> suspendedOrgs++;
                default -> {}
            }
        }

        return PlatformDashboardResponse.builder()
                .totalOrganizations(totalOrgs)
                .activeOrganizations(activeOrgs)
                .trialOrganizations(trialOrgs)
                .suspendedOrganizations(suspendedOrgs)
                .totalStores(totalStores)
                .totalUsers(totalUsers)
                .totalCustomers(totalCustomers)
                .planBreakdown(planBreakdown)
                .build();
    }
}
