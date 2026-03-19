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

import com.aurajewels.jewel.dto.admin.OnboardOrgRequest;
import com.aurajewels.jewel.dto.admin.OnboardOrgResponse;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.*;
import com.aurajewels.jewel.security.StoreContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class OrganizationAdminService {

    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final UserStoreAccessRepository userStoreAccessRepository;
    private final FeatureModuleRepository featureModuleRepository;
    private final StoreFeatureModuleRepository storeFeatureModuleRepository;
    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final PlatformAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Organization> listOrganizations() {
        return organizationRepository.findAll();
    }

    public Organization getOrganization(Long id) {
        return organizationRepository
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + id));
    }

    @Transactional
    public OnboardOrgResponse onboard(OnboardOrgRequest request) {
        Long adminId = StoreContext.getCurrentUserId();

        // 1. Create organization
        Organization org =
                Organization.builder()
                        .name(request.getOrganization().getName())
                        .onboardedBy(adminId)
                        .onboardedAt(Instant.now())
                        .status("ACTIVE")
                        .build();
        org = organizationRepository.save(org);

        // 2. Create stores
        List<Store> stores = new ArrayList<>();
        for (OnboardOrgRequest.StoreInfo storeInfo : request.getStores()) {
            Store store =
                    Store.builder()
                            .organization(org)
                            .name(storeInfo.getName())
                            .address(storeInfo.getAddress())
                            .city(storeInfo.getCity())
                            .phone(storeInfo.getPhone())
                            .gstin(storeInfo.getGstin())
                            .status("ACTIVE")
                            .build();
            stores.add(storeRepository.save(store));
        }

        // 3. Create owner user
        User owner =
                User.builder()
                        .organization(org)
                        .name(request.getOwner().getName())
                        .mobile(request.getOwner().getMobile())
                        .email(request.getOwner().getEmail())
                        .passwordHash(passwordEncoder.encode(request.getOwner().getPassword()))
                        .role(Role.OWNER)
                        .forcePasswordChange(true)
                        .build();
        owner = userRepository.save(owner);

        // 4. Create user_store_access for owner → all stores
        for (Store store : stores) {
            UserStoreAccess access = UserStoreAccess.builder().user(owner).store(store).build();
            userStoreAccessRepository.save(access);
        }

        // 5. Create subscription
        int trialDays =
                request.getSubscription() != null
                                && request.getSubscription().getTrialDays() != null
                        ? request.getSubscription().getTrialDays()
                        : 30;
        String planStr =
                request.getSubscription() != null && request.getSubscription().getPlan() != null
                        ? request.getSubscription().getPlan()
                        : "STARTER";
        int maxStores =
                request.getSubscription() != null
                                && request.getSubscription().getMaxStores() != null
                        ? request.getSubscription().getMaxStores()
                        : 1;
        int maxUsers =
                request.getSubscription() != null && request.getSubscription().getMaxUsers() != null
                        ? request.getSubscription().getMaxUsers()
                        : 2;

        OrgSubscription subscription =
                OrgSubscription.builder()
                        .organization(org)
                        .plan(SubscriptionPlan.valueOf(planStr))
                        .status(SubscriptionStatus.TRIAL)
                        .trialEndsAt(Instant.now().plus(trialDays, ChronoUnit.DAYS))
                        .maxStores(maxStores)
                        .maxUsers(maxUsers)
                        .build();
        orgSubscriptionRepository.save(subscription);

        // 6. Enable feature modules per store
        List<FeatureModule> allModules =
                featureModuleRepository.findByActiveTrueOrderBySortOrderAsc();
        for (int i = 0; i < stores.size(); i++) {
            Store store = stores.get(i);
            OnboardOrgRequest.StoreInfo storeInfo = request.getStores().get(i);
            List<String> moduleCodes = storeInfo.getModules();

            for (FeatureModule module : allModules) {
                boolean shouldEnable =
                        module.getIsCore()
                                || (moduleCodes != null && moduleCodes.contains(module.getCode()));
                StoreFeatureModule sfm =
                        StoreFeatureModule.builder()
                                .store(store)
                                .module(module)
                                .enabled(shouldEnable)
                                .enabledBy(adminId)
                                .build();
                storeFeatureModuleRepository.save(sfm);
            }
        }

        // 7. Audit log
        auditLogRepository.save(
                PlatformAuditLog.builder()
                        .adminId(adminId)
                        .adminName("Super Admin")
                        .action("ONBOARD_ORG")
                        .entityType("ORGANIZATION")
                        .entityId(org.getId())
                        .detail(
                                "Onboarded org: "
                                        + org.getName()
                                        + " with "
                                        + stores.size()
                                        + " stores")
                        .build());

        // 8. Build response
        List<OnboardOrgResponse.StoreRef> storeRefs =
                stores.stream()
                        .map(
                                s ->
                                        OnboardOrgResponse.StoreRef.builder()
                                                .id(s.getId())
                                                .name(s.getName())
                                                .build())
                        .toList();

        return OnboardOrgResponse.builder()
                .organizationId(org.getId())
                .organizationName(org.getName())
                .ownerId(owner.getId())
                .ownerMobile(owner.getMobile())
                .ownerTempPassword(request.getOwner().getPassword())
                .forcePasswordChange(true)
                .stores(storeRefs)
                .subscription(
                        OnboardOrgResponse.SubscriptionRef.builder()
                                .plan(subscription.getPlan().name())
                                .status(subscription.getStatus().name())
                                .trialEndsAt(subscription.getTrialEndsAt())
                                .build())
                .build();
    }

    @Transactional
    public Organization updateStatus(Long orgId, String status) {
        Organization org = getOrganization(orgId);
        org.setStatus(status);
        return organizationRepository.save(org);
    }

    /** Get stores, owner, subscription for an org. */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrgDetail(Long orgId) {
        Organization org = getOrganization(orgId);
        List<Store> stores = storeRepository.findByOrganizationId(orgId);
        OrgSubscription sub = orgSubscriptionRepository.findByOrganizationId(orgId).orElse(null);
        List<User> users = userRepository.findByOrganizationId(orgId);
        User owner = users.stream().filter(u -> u.getRole() == Role.OWNER).findFirst().orElse(null);

        return Map.of(
                "organization",
                org,
                "stores",
                stores,
                "owner",
                owner != null
                        ? Map.of(
                                "id",
                                owner.getId(),
                                "name",
                                owner.getName(),
                                "mobile",
                                owner.getMobile())
                        : Map.of(),
                "subscription",
                sub != null ? sub : Map.of(),
                "totalUsers",
                users.size());
    }
}
