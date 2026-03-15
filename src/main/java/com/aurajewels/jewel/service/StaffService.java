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

import com.aurajewels.jewel.dto.staff.CreateStaffRequest;
import com.aurajewels.jewel.dto.staff.StaffResponse;
import com.aurajewels.jewel.dto.staff.UpdateStaffRequest;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.*;
import com.aurajewels.jewel.security.StoreContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final OrganizationRepository organizationRepository;
    private final PermissionRepository permissionRepository;
    private final UserStoreAccessRepository userStoreAccessRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<StaffResponse> listStaff() {
        Long orgId = StoreContext.getCurrentOrgId();
        List<User> users = userRepository.findByOrganizationIdAndActiveTrue(orgId);
        return users.stream().map(this::toStaffResponse).toList();
    }

    @Transactional
    public StaffResponse createStaff(CreateStaffRequest request) {
        Long orgId = StoreContext.getCurrentOrgId();
        String callerRole = StoreContext.getCurrentRole();

        // Validate role assignment
        Role requestedRole = Role.valueOf(request.getRole().toUpperCase());
        if (requestedRole == Role.OWNER) {
            throw new IllegalArgumentException("Cannot create another OWNER");
        }
        if (requestedRole == Role.ADMIN && !"OWNER".equals(callerRole)) {
            throw new IllegalArgumentException("Only OWNER can create ADMIN users");
        }

        Organization org =
                organizationRepository
                        .findById(orgId)
                        .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        // Create user
        User user =
                User.builder()
                        .organization(org)
                        .name(request.getName())
                        .mobile(request.getMobile())
                        .passwordHash(passwordEncoder.encode(request.getPassword()))
                        .role(requestedRole)
                        .forcePasswordChange(true)
                        .build();
        user.setActive(true);
        user = userRepository.save(user);

        // Assign store access
        if (request.getStoreIds() != null && !request.getStoreIds().isEmpty()) {
            for (Long storeId : request.getStoreIds()) {
                Store store =
                        storeRepository
                                .findById(storeId)
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "Store not found: " + storeId));
                UserStoreAccess access = UserStoreAccess.builder().user(user).store(store).build();
                userStoreAccessRepository.save(access);
            }
        }

        // Assign permissions (only for STAFF)
        if (requestedRole == Role.STAFF
                && request.getPermissions() != null
                && !request.getPermissions().isEmpty()) {
            List<Permission> perms = permissionRepository.findByNameIn(request.getPermissions());
            for (Permission perm : perms) {
                if (request.getStoreIds() != null) {
                    for (Long storeId : request.getStoreIds()) {
                        Store store = storeRepository.findById(storeId).orElse(null);
                        if (store != null) {
                            UserPermission up =
                                    UserPermission.builder()
                                            .user(user)
                                            .store(store)
                                            .permission(perm)
                                            .build();
                            userPermissionRepository.save(up);
                        }
                    }
                }
            }
        }

        return toStaffResponse(user);
    }

    @Transactional
    public StaffResponse updateStaff(Long userId, UpdateStaffRequest request) {
        Long orgId = StoreContext.getCurrentOrgId();
        User user =
                userRepository
                        .findByIdAndOrganizationId(userId, orgId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == Role.OWNER) {
            throw new IllegalArgumentException("Cannot update OWNER via this endpoint");
        }

        if (request.getName() != null) user.setName(request.getName());
        if (request.getMobile() != null) user.setMobile(request.getMobile());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getSalary() != null) user.setSalary(request.getSalary());
        if (request.getCommission() != null) user.setCommission(request.getCommission());
        if (request.getSalesTarget() != null) user.setSalesTarget(request.getSalesTarget());

        if (request.getStatus() != null) {
            user.setActive("ACTIVE".equalsIgnoreCase(request.getStatus()));
        }

        if (request.getRole() != null) {
            Role newRole = Role.valueOf(request.getRole().toUpperCase());
            if (newRole == Role.OWNER) {
                throw new IllegalArgumentException("Cannot assign OWNER role");
            }
            String callerRole = StoreContext.getCurrentRole();
            if (newRole == Role.ADMIN && !"OWNER".equals(callerRole)) {
                throw new IllegalArgumentException("Only OWNER can assign ADMIN role");
            }
            user.setRole(newRole);
        }

        userRepository.save(user);

        // Update store access if provided
        if (request.getStoreIds() != null) {
            userStoreAccessRepository.deleteByUserId(user.getId());
            userStoreAccessRepository.flush();
            for (Long storeId : request.getStoreIds()) {
                Store store =
                        storeRepository
                                .findById(storeId)
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "Store not found: " + storeId));
                UserStoreAccess access = UserStoreAccess.builder().user(user).store(store).build();
                userStoreAccessRepository.save(access);
            }
        }

        // Update permissions if provided (only meaningful for STAFF)
        if (request.getPermissions() != null) {
            userPermissionRepository.deleteByUserId(user.getId());
            userPermissionRepository.flush();
            if (user.getRole() == Role.STAFF) {
                List<Permission> perms =
                        permissionRepository.findByNameIn(request.getPermissions());
                List<Long> storeIds =
                        request.getStoreIds() != null
                                ? request.getStoreIds()
                                : userStoreAccessRepository.findByUserId(user.getId()).stream()
                                        .map(sa -> sa.getStore().getId())
                                        .toList();
                for (Permission perm : perms) {
                    for (Long storeId : storeIds) {
                        Store store = storeRepository.findById(storeId).orElse(null);
                        if (store != null) {
                            UserPermission up =
                                    UserPermission.builder()
                                            .user(user)
                                            .store(store)
                                            .permission(perm)
                                            .build();
                            userPermissionRepository.save(up);
                        }
                    }
                }
            }
        }

        // Re-fetch user to get the latest state after persistence context clears
        user =
                userRepository
                        .findByIdAndOrganizationId(userId, orgId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return toStaffResponse(user);
    }

    @Transactional
    public void deactivateStaff(Long userId) {
        Long orgId = StoreContext.getCurrentOrgId();
        User user =
                userRepository
                        .findByIdAndOrganizationId(userId, orgId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getRole() == Role.OWNER) {
            throw new IllegalArgumentException("Cannot deactivate OWNER");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public StaffResponse getStaff(Long userId) {
        Long orgId = StoreContext.getCurrentOrgId();
        User user =
                userRepository
                        .findByIdAndOrganizationId(userId, orgId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toStaffResponse(user);
    }

    private StaffResponse toStaffResponse(User user) {
        List<UserStoreAccess> storeAccess = userStoreAccessRepository.findByUserId(user.getId());
        List<UserPermission> perms = userPermissionRepository.findByUserId(user.getId());

        return StaffResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .mobile(user.getMobile())
                .email(user.getEmail())
                .role(user.getRole().name())
                .salary(user.getSalary())
                .commission(user.getCommission())
                .salesTarget(user.getSalesTarget())
                .active(user.getActive() != null && user.getActive())
                .stores(storeAccess.stream().map(sa -> sa.getStore().getName()).toList())
                .permissions(
                        perms.stream().map(up -> up.getPermission().getName()).distinct().toList())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
