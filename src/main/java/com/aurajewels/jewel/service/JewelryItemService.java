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

import com.aurajewels.jewel.entity.Category;
import com.aurajewels.jewel.entity.JewelryItem;
import com.aurajewels.jewel.entity.MetalType;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.CategoryRepository;
import com.aurajewels.jewel.repository.JewelryItemRepository;
import com.aurajewels.jewel.repository.MetalTypeRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jewelry item service for managing inventory items with category and metal type lookups.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JewelryItemService {

    private final JewelryItemRepository jewelryItemRepository;
    private final StoreRepository storeRepository;
    private final MetalTypeRepository metalTypeRepository;
    private final CategoryRepository categoryRepository;

    public List<JewelryItem> findAll() {
        Long storeId = StoreContext.getCurrentStoreId();
        return jewelryItemRepository.findByStoreIdAndActiveTrue(storeId);
    }

    public JewelryItem findById(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return jewelryItemRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("JewelryItem not found with id: " + id));
    }

    public JewelryItem findBySku(String sku) {
        Long storeId = StoreContext.getCurrentStoreId();
        return jewelryItemRepository
                .findBySkuAndStoreId(sku, storeId)
                .orElseThrow(() -> new RuntimeException("JewelryItem not found with SKU: " + sku));
    }

    public List<JewelryItem> findByCategory(Long categoryId) {
        Long storeId = StoreContext.getCurrentStoreId();
        return jewelryItemRepository.findByCategoryIdAndStoreIdAndActiveTrue(categoryId, storeId);
    }

    public List<JewelryItem> findByStatus(JewelryItem.ItemStatus status) {
        Long storeId = StoreContext.getCurrentStoreId();
        return jewelryItemRepository.findByStatusAndStoreIdAndActiveTrue(status, storeId);
    }

    @Transactional
    public JewelryItem create(JewelryItem item) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new RuntimeException("Store not found"));
        item.setStore(store);

        // Reject cross-store linkage: the metal type and category must belong to this store,
        // otherwise an item can end up billing off another store's rate/purity.
        item.setMetalType(resolveMetalType(item.getMetalType(), storeId));
        item.setCategory(resolveCategory(item.getCategory(), storeId));

        // Validate HUID uniqueness within the store (if provided)
        if (item.getHuid() != null && !item.getHuid().isBlank()) {
            boolean exists =
                    jewelryItemRepository.existsByHuidAndStoreIdAndActiveTrue(
                            item.getHuid(), storeId);
            if (exists) {
                throw new IllegalArgumentException(
                        "HUID already exists in this store: " + item.getHuid());
            }
        }

        // Set back-references on stone details so JPA can persist them via cascade
        if (item.getStoneDetails() != null) {
            item.getStoneDetails().forEach(stone -> stone.setJewelryItem(item));
        }

        return jewelryItemRepository.save(item);
    }

    @Transactional
    public JewelryItem update(Long id, JewelryItem updated) {
        JewelryItem existing = findById(id);
        Long storeId = existing.getStore().getId();
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setGrossWeight(updated.getGrossWeight());
        existing.setNetWeight(updated.getNetWeight());
        existing.setStoneWeight(updated.getStoneWeight());
        existing.setMakingCharges(updated.getMakingCharges());
        existing.setStoneCharges(updated.getStoneCharges());
        existing.setOtherCharges(updated.getOtherCharges());
        existing.setQuantity(updated.getQuantity());
        existing.setHsnCode(updated.getHsnCode());
        existing.setBarcode(updated.getBarcode());
        existing.setHuid(updated.getHuid());
        existing.setHallmarkCert(updated.getHallmarkCert());
        existing.setHallmarkDate(updated.getHallmarkDate());
        existing.setShowcaseLocation(updated.getShowcaseLocation());
        existing.setImageUrl(updated.getImageUrl());
        existing.setStatus(updated.getStatus());
        existing.setCategory(resolveCategory(updated.getCategory(), storeId));
        existing.setMetalType(resolveMetalType(updated.getMetalType(), storeId));
        if (updated.getArEnabled() != null) existing.setArEnabled(updated.getArEnabled());
        if (updated.getArType() != null) existing.setArType(updated.getArType());

        // Replace stone details (orphanRemoval = true auto-deletes removed stones)
        if (updated.getStoneDetails() != null) {
            existing.replaceStoneDetails(updated.getStoneDetails());
        }

        return jewelryItemRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        JewelryItem existing = findById(id);
        existing.setActive(false);
        jewelryItemRepository.save(existing);
    }

    /**
     * Resolve the incoming metal type to a managed entity that belongs to {@code storeId}, rejecting
     * any reference to another store's metal type (the root cause of 22K stock billing at the 24K
     * rate of a different store).
     */
    private MetalType resolveMetalType(MetalType incoming, Long storeId) {
        if (incoming == null || incoming.getId() == null) {
            throw new IllegalArgumentException("Metal type is required");
        }
        return metalTypeRepository
                .findByIdAndStoreId(incoming.getId(), storeId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Metal type "
                                                + incoming.getId()
                                                + " does not belong to store "
                                                + storeId));
    }

    /** Resolve the incoming category to a managed entity that belongs to {@code storeId}. */
    private Category resolveCategory(Category incoming, Long storeId) {
        if (incoming == null || incoming.getId() == null) {
            throw new IllegalArgumentException("Category is required");
        }
        return categoryRepository
                .findByIdAndStoreId(incoming.getId(), storeId)
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Category "
                                                + incoming.getId()
                                                + " does not belong to store "
                                                + storeId));
    }
}
