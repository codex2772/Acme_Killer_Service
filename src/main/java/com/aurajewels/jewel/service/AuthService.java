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

import com.aurajewels.jewel.dto.auth.*;
import com.aurajewels.jewel.entity.Role;
import com.aurajewels.jewel.entity.User;
import com.aurajewels.jewel.entity.UserStoreAccess;
import com.aurajewels.jewel.repository.StoreFeatureModuleRepository;
import com.aurajewels.jewel.repository.UserPermissionRepository;
import com.aurajewels.jewel.repository.UserRepository;
import com.aurajewels.jewel.repository.UserStoreAccessRepository;
import com.aurajewels.jewel.security.JwtUtil;
import java.util.Collections;
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
public class AuthService {

    private final UserRepository userRepository;
    private final UserStoreAccessRepository userStoreAccessRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final StoreFeatureModuleRepository storeFeatureModuleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user =
                userRepository
                        .findByMobileAndActiveTrue(request.getMobile())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Invalid mobile or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid mobile or password");
        }

        // Get user's accessible stores
        List<UserStoreAccess> storeAccess = userStoreAccessRepository.findByUserId(user.getId());
        if (storeAccess.isEmpty()) {
            throw new IllegalArgumentException("No store access configured for this user");
        }

        // Default to first store
        Long defaultStoreId = storeAccess.get(0).getStore().getId();

        // Get permissions for the default store
        List<String> permissions = getPermissionsForUserAndStore(user, defaultStoreId);

        // Generate tokens
        String token = jwtUtil.generateToken(user, defaultStoreId, permissions);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Build store list with enabled modules
        List<StoreDto> stores =
                storeAccess.stream()
                        .map(
                                sa -> {
                                    List<String> modules =
                                            storeFeatureModuleRepository
                                                    .findEnabledModuleCodesByStoreId(
                                                            sa.getStore().getId());
                                    return StoreDto.builder()
                                            .id(sa.getStore().getId())
                                            .name(sa.getStore().getName())
                                            .city(sa.getStore().getCity())
                                            .enabledModules(modules)
                                            .build();
                                })
                        .toList();

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .userName(user.getName())
                .forcePasswordChange(
                        user.getForcePasswordChange() != null && user.getForcePasswordChange())
                .stores(stores)
                .build();
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setForcePasswordChange(false);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<UserStoreAccess> storeAccess = userStoreAccessRepository.findByUserId(user.getId());
        Long defaultStoreId = storeAccess.get(0).getStore().getId();
        List<String> permissions = getPermissionsForUserAndStore(user, defaultStoreId);

        String newToken = jwtUtil.generateToken(user, defaultStoreId, permissions);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);

        List<StoreDto> stores =
                storeAccess.stream()
                        .map(
                                sa -> {
                                    List<String> modules =
                                            storeFeatureModuleRepository
                                                    .findEnabledModuleCodesByStoreId(
                                                            sa.getStore().getId());
                                    return StoreDto.builder()
                                            .id(sa.getStore().getId())
                                            .name(sa.getStore().getName())
                                            .city(sa.getStore().getCity())
                                            .enabledModules(modules)
                                            .build();
                                })
                        .toList();

        return LoginResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .role(user.getRole().name())
                .userName(user.getName())
                .forcePasswordChange(false)
                .stores(stores)
                .build();
    }

    private List<String> getPermissionsForUserAndStore(User user, Long storeId) {
        if (user.getRole() == Role.OWNER || user.getRole() == Role.ADMIN) {
            return Collections.emptyList(); // They bypass permission checks
        }
        return userPermissionRepository.findPermissionNamesByUserIdAndStoreId(
                user.getId(), storeId);
    }
}
