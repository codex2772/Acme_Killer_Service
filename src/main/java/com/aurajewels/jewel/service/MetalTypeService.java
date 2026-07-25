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

import com.aurajewels.jewel.entity.MetalType;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.MetalTypeRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Metal type service for managing metal types and current rates.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetalTypeService {

    private final MetalTypeRepository metalTypeRepository;
    private final StoreRepository storeRepository;

    /** Single source of truth for the metal-type set every store is provisioned with. */
    public record DefaultMetalType(String name, String purity, BigDecimal currentRate) {}

    /**
     * Standard metal types seeded for every new store (rates mirror the demo seed in V1, with
     * Gold 14K added). Kept in sync with the V16 backfill migration.
     */
    public static final List<DefaultMetalType> DEFAULT_METAL_TYPES =
            List.of(
                    new DefaultMetalType("Gold", "24K", new BigDecimal("7500.00")),
                    new DefaultMetalType("Gold", "22K", new BigDecimal("6900.00")),
                    new DefaultMetalType("Gold", "18K", new BigDecimal("5625.00")),
                    new DefaultMetalType("Gold", "14K", new BigDecimal("4375.00")),
                    new DefaultMetalType("Silver", "925", new BigDecimal("95.00")),
                    new DefaultMetalType("Platinum", "950", new BigDecimal("3200.00")));

    public List<MetalType> findAll() {
        Long storeId = StoreContext.getCurrentStoreId();
        return metalTypeRepository.findByStoreIdAndActiveTrue(storeId);
    }

    public MetalType findById(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return metalTypeRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("MetalType not found with id: " + id));
    }

    public List<MetalType> findByName(String name) {
        Long storeId = StoreContext.getCurrentStoreId();
        return metalTypeRepository.findByNameAndStoreIdAndActiveTrue(name, storeId);
    }

    /**
     * Provision the standard metal-type set for a freshly created store so items can always be
     * linked to a correct in-store metal type. Skips any (name, purity) that already exists, so it
     * is safe to call more than once.
     */
    @Transactional
    public void provisionDefaults(Store store) {
        for (DefaultMetalType def : DEFAULT_METAL_TYPES) {
            boolean exists =
                    metalTypeRepository
                            .findByNameAndStoreIdAndActiveTrue(def.name(), store.getId())
                            .stream()
                            .anyMatch(mt -> def.purity().equals(mt.getPurity()));
            if (exists) {
                continue;
            }
            metalTypeRepository.save(
                    MetalType.builder()
                            .store(store)
                            .name(def.name())
                            .purity(def.purity())
                            .currentRate(def.currentRate())
                            .unit("gram")
                            .build());
        }
    }

    @Transactional
    public MetalType create(MetalType metalType) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new RuntimeException("Store not found"));
        metalType.setStore(store);
        return metalTypeRepository.save(metalType);
    }

    @Transactional
    public MetalType update(Long id, MetalType updated) {
        MetalType existing = findById(id);
        existing.setName(updated.getName());
        existing.setPurity(updated.getPurity());
        existing.setCurrentRate(updated.getCurrentRate());
        existing.setUnit(updated.getUnit());
        return metalTypeRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        MetalType existing = findById(id);
        existing.setActive(false);
        metalTypeRepository.save(existing);
    }
}
