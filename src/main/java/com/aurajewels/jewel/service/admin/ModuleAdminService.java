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

import com.aurajewels.jewel.dto.admin.StoreModulesResponse;
import com.aurajewels.jewel.entity.FeatureModule;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.entity.StoreFeatureModule;
import com.aurajewels.jewel.repository.FeatureModuleRepository;
import com.aurajewels.jewel.repository.StoreFeatureModuleRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class ModuleAdminService {

    private final FeatureModuleRepository featureModuleRepository;
    private final StoreFeatureModuleRepository storeFeatureModuleRepository;
    private final StoreRepository storeRepository;

    public List<FeatureModule> listAllModules() {
        return featureModuleRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public StoreModulesResponse getStoreModules(Long storeId) {
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Store not found: " + storeId));

        List<FeatureModule> allModules =
                featureModuleRepository.findByActiveTrueOrderBySortOrderAsc();
        Set<String> enabledCodes =
                storeFeatureModuleRepository.findEnabledModuleCodesByStoreId(storeId).stream()
                        .collect(Collectors.toSet());

        List<StoreModulesResponse.ModuleRef> enabled = new ArrayList<>();
        List<StoreModulesResponse.ModuleRef> disabled = new ArrayList<>();

        for (FeatureModule m : allModules) {
            StoreModulesResponse.ModuleRef ref =
                    StoreModulesResponse.ModuleRef.builder()
                            .code(m.getCode())
                            .name(m.getName())
                            .isCore(m.getIsCore())
                            .build();
            if (enabledCodes.contains(m.getCode())) {
                enabled.add(ref);
            } else {
                disabled.add(ref);
            }
        }

        return StoreModulesResponse.builder()
                .storeId(storeId)
                .storeName(store.getName())
                .enabledModules(enabled)
                .disabledModules(disabled)
                .build();
    }

    @Transactional
    public StoreModulesResponse setStoreModules(Long storeId, List<String> moduleCodes) {
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Store not found: " + storeId));

        Long adminId = StoreContext.getCurrentUserId();
        List<FeatureModule> allModules =
                featureModuleRepository.findByActiveTrueOrderBySortOrderAsc();

        // Delete existing and re-insert
        storeFeatureModuleRepository.deleteByStoreId(storeId);
        storeFeatureModuleRepository.flush();

        for (FeatureModule module : allModules) {
            boolean shouldEnable = module.getIsCore() || moduleCodes.contains(module.getCode());
            StoreFeatureModule sfm =
                    StoreFeatureModule.builder()
                            .store(store)
                            .module(module)
                            .enabled(shouldEnable)
                            .enabledBy(adminId)
                            .build();
            storeFeatureModuleRepository.save(sfm);
        }

        return getStoreModules(storeId);
    }

    @Transactional
    public StoreModulesResponse enableModule(Long storeId, String moduleCode) {
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Store not found: " + storeId));
        FeatureModule module =
                featureModuleRepository
                        .findByCode(moduleCode)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Module not found: " + moduleCode));

        storeFeatureModuleRepository
                .findByStoreIdAndModuleCode(storeId, moduleCode)
                .ifPresentOrElse(
                        sfm -> {
                            sfm.setEnabled(true);
                            storeFeatureModuleRepository.save(sfm);
                        },
                        () -> {
                            StoreFeatureModule sfm =
                                    StoreFeatureModule.builder()
                                            .store(store)
                                            .module(module)
                                            .enabled(true)
                                            .enabledBy(StoreContext.getCurrentUserId())
                                            .build();
                            storeFeatureModuleRepository.save(sfm);
                        });

        return getStoreModules(storeId);
    }

    @Transactional
    public StoreModulesResponse disableModule(Long storeId, String moduleCode) {
        FeatureModule module =
                featureModuleRepository
                        .findByCode(moduleCode)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Module not found: " + moduleCode));

        if (module.getIsCore()) {
            throw new IllegalArgumentException("Cannot disable core module: " + moduleCode);
        }

        storeFeatureModuleRepository
                .findByStoreIdAndModuleCode(storeId, moduleCode)
                .ifPresent(
                        sfm -> {
                            sfm.setEnabled(false);
                            storeFeatureModuleRepository.save(sfm);
                        });

        return getStoreModules(storeId);
    }
}
