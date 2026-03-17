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

import com.aurajewels.jewel.entity.JewelryItem;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.JewelryItemRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JewelryItemService {

    private final JewelryItemRepository jewelryItemRepository;
    private final StoreRepository storeRepository;

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
        return jewelryItemRepository.save(item);
    }

    @Transactional
    public JewelryItem update(Long id, JewelryItem updated) {
        JewelryItem existing = findById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setGrossWeight(updated.getGrossWeight());
        existing.setNetWeight(updated.getNetWeight());
        existing.setMakingCharges(updated.getMakingCharges());
        existing.setStoneCharges(updated.getStoneCharges());
        existing.setOtherCharges(updated.getOtherCharges());
        existing.setQuantity(updated.getQuantity());
        existing.setHsnCode(updated.getHsnCode());
        existing.setBarcode(updated.getBarcode());
        existing.setStatus(updated.getStatus());
        existing.setCategory(updated.getCategory());
        existing.setMetalType(updated.getMetalType());
        return jewelryItemRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        JewelryItem existing = findById(id);
        existing.setActive(false);
        jewelryItemRepository.save(existing);
    }
}
