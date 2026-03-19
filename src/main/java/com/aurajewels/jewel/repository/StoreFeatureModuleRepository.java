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
package com.aurajewels.jewel.repository;

import com.aurajewels.jewel.entity.StoreFeatureModule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Raviraj Bhosale
 */
@Repository
public interface StoreFeatureModuleRepository extends JpaRepository<StoreFeatureModule, Long> {

    List<StoreFeatureModule> findByStoreId(Long storeId);

    List<StoreFeatureModule> findByStoreIdAndEnabledTrue(Long storeId);

    @Query(
            "SELECT sfm FROM StoreFeatureModule sfm WHERE sfm.store.id = :storeId AND sfm.module.code = :moduleCode")
    Optional<StoreFeatureModule> findByStoreIdAndModuleCode(
            @Param("storeId") Long storeId, @Param("moduleCode") String moduleCode);

    @Query(
            "SELECT sfm.module.code FROM StoreFeatureModule sfm WHERE sfm.store.id = :storeId AND sfm.enabled = true")
    List<String> findEnabledModuleCodesByStoreId(@Param("storeId") Long storeId);

    @Query(
            "SELECT CASE WHEN COUNT(sfm) > 0 THEN true ELSE false END FROM StoreFeatureModule sfm WHERE sfm.store.id = :storeId AND sfm.module.code = :moduleCode AND sfm.enabled = true")
    boolean existsByStoreIdAndModuleCodeAndEnabledTrue(
            @Param("storeId") Long storeId, @Param("moduleCode") String moduleCode);

    @Modifying
    @Query("DELETE FROM StoreFeatureModule sfm WHERE sfm.store.id = :storeId")
    void deleteByStoreId(@Param("storeId") Long storeId);
}
