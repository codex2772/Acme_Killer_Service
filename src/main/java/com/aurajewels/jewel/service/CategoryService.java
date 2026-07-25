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
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.CategoryRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Category service for managing jewelry categories.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    /** Single source of truth for the category set every store is provisioned with. */
    public record DefaultCategory(String name, String description) {}

    /**
     * Standard categories seeded for every new store (mirrors the Store 1 demo seed in V1). Names
     * are chosen so the desktop Add-Inventory dropdown resolves them by name match. Kept in sync
     * with the V17 seed migration.
     */
    public static final List<DefaultCategory> DEFAULT_CATEGORIES =
            List.of(
                    new DefaultCategory("Rings", "All types of rings"),
                    new DefaultCategory("Necklaces", "Chains and necklaces"),
                    new DefaultCategory("Earrings", "Studs, drops, and hoops"),
                    new DefaultCategory("Bangles", "Bangles and bracelets"),
                    new DefaultCategory("Pendants", "Pendants and lockets"),
                    new DefaultCategory("Chains", "Gold and silver chains"),
                    new DefaultCategory("Anklets", "Anklets and payal"));

    public List<Category> findAll() {
        Long storeId = StoreContext.getCurrentStoreId();
        return categoryRepository.findByStoreIdAndActiveTrue(storeId);
    }

    public Category findById(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return categoryRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));
    }

    /**
     * Provision the standard category set for a freshly created store so items can always be linked
     * to an in-store category. Skips any name that already exists, so it is safe to call repeatedly.
     */
    @Transactional
    public void provisionDefaults(Store store) {
        for (DefaultCategory def : DEFAULT_CATEGORIES) {
            if (categoryRepository.findByNameAndStoreId(def.name(), store.getId()).isPresent()) {
                continue;
            }
            categoryRepository.save(
                    Category.builder()
                            .store(store)
                            .name(def.name())
                            .description(def.description())
                            .build());
        }
    }

    @Transactional
    public Category create(Category category) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new RuntimeException("Store not found"));
        category.setStore(store);
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, Category updated) {
        Category existing = findById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        return categoryRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Category existing = findById(id);
        existing.setActive(false);
        categoryRepository.save(existing);
    }
}
