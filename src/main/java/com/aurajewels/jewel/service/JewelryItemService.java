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

import com.aurajewels.jewel.entity.JewelryItem;
import com.aurajewels.jewel.repository.JewelryItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JewelryItemService {

    private final JewelryItemRepository jewelryItemRepository;

    public List<JewelryItem> findAll() {
        return jewelryItemRepository.findByActiveTrue();
    }

    public JewelryItem findById(Long id) {
        return jewelryItemRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("JewelryItem not found with id: " + id));
    }

    public JewelryItem findBySku(String sku) {
        return jewelryItemRepository
                .findBySku(sku)
                .orElseThrow(() -> new RuntimeException("JewelryItem not found with SKU: " + sku));
    }

    public List<JewelryItem> findByCategory(Long categoryId) {
        return jewelryItemRepository.findByCategoryIdAndActiveTrue(categoryId);
    }

    public List<JewelryItem> findByStatus(JewelryItem.ItemStatus status) {
        return jewelryItemRepository.findByStatusAndActiveTrue(status);
    }

    @Transactional
    public JewelryItem create(JewelryItem item) {
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
        return jewelryItemRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        JewelryItem existing = findById(id);
        existing.setActive(false);
        jewelryItemRepository.save(existing);
    }
}
