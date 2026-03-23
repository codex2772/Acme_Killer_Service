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

import com.aurajewels.jewel.entity.ArAsset;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for ArAsset entities.
 *
 * @author Raviraj Bhosale
 */
@Repository
public interface ArAssetRepository extends JpaRepository<ArAsset, Long> {

    Optional<ArAsset> findByJewelryItemId(Long jewelryItemId);

    Optional<ArAsset> findByJewelryItemIdAndStoreId(Long jewelryItemId, Long storeId);

    List<ArAsset> findByStoreIdAndStatus(Long storeId, ArAsset.ArAssetStatus status);

    List<ArAsset> findByStoreIdAndActiveTrue(Long storeId);

    List<ArAsset> findByArTypeAndStoreIdAndStatus(
            ArAsset.ArType arType, Long storeId, ArAsset.ArAssetStatus status);

    @Query(
            "SELECT a FROM ArAsset a WHERE a.store.id = :storeId "
                    + "AND a.status = 'READY' AND a.active = true")
    List<ArAsset> findReadyByStoreId(@Param("storeId") Long storeId);

    long countByStoreIdAndStatus(Long storeId, ArAsset.ArAssetStatus status);
}
