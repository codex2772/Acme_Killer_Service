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

import com.aurajewels.jewel.dto.ar.*;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.*;
import com.aurajewels.jewel.security.StoreContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AR virtual try-on service for managing overlay assets, try-on sessions, and analytics.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class ArService {

    private final ArAssetRepository arAssetRepository;
    private final ArSessionRepository arSessionRepository;
    private final JewelryItemRepository jewelryItemRepository;
    private final StoreRepository storeRepository;
    private final ActivityLogService activityLogService;

    // ════════════════════════════════════════
    // AR Asset CRUD
    // ════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ArAssetResponse> listArAssets(String arType, String status) {
        Long storeId = StoreContext.getCurrentStoreId();
        List<ArAsset> assets;

        if (status != null && arType != null) {
            assets =
                    arAssetRepository.findByArTypeAndStoreIdAndStatus(
                            ArAsset.ArType.valueOf(arType),
                            storeId,
                            ArAsset.ArAssetStatus.valueOf(status));
        } else if (status != null) {
            assets =
                    arAssetRepository.findByStoreIdAndStatus(
                            storeId, ArAsset.ArAssetStatus.valueOf(status));
        } else {
            assets = arAssetRepository.findByStoreIdAndActiveTrue(storeId);
        }

        return assets.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ArAssetResponse getArAsset(Long itemId) {
        Long storeId = StoreContext.getCurrentStoreId();
        ArAsset asset =
                arAssetRepository
                        .findByJewelryItemIdAndStoreId(itemId, storeId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "AR asset not found for item: " + itemId));
        return toResponse(asset);
    }

    @Transactional
    public ArAssetResponse createOrUpdateArAsset(ArAssetRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        JewelryItem item =
                jewelryItemRepository
                        .findByIdAndStoreId(request.getJewelryItemId(), storeId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Jewelry item not found"));

        // Upsert — create or update existing
        ArAsset asset =
                arAssetRepository
                        .findByJewelryItemId(request.getJewelryItemId())
                        .orElse(
                                ArAsset.builder()
                                        .jewelryItem(item)
                                        .store(store)
                                        .source(ArAsset.ArAssetSource.MANUAL_UPLOAD)
                                        .build());

        asset.setArType(ArAsset.ArType.valueOf(request.getArType()));
        asset.setOverlayUrl(request.getOverlayUrl());
        asset.setModelUrl(request.getModelUrl());
        asset.setThumbnailUrl(request.getThumbnailUrl());
        asset.setAnchorConfig(request.getAnchorConfig());
        asset.setWidth(request.getWidth());
        asset.setHeight(request.getHeight());
        asset.setStatus(ArAsset.ArAssetStatus.READY);
        asset.setActive(true);

        arAssetRepository.save(asset);

        // Update the jewelry item's ar_enabled flag
        item.setArEnabled(true);
        item.setArType(ArAsset.ArType.valueOf(request.getArType()));
        jewelryItemRepository.save(item);

        activityLogService.log(
                "AR Asset Created/Updated",
                "AR overlay for " + item.getName() + " (" + request.getArType() + ")",
                "AR",
                "AR_ASSET",
                asset.getId());

        return toResponse(asset);
    }

    @Transactional
    public void deleteArAsset(Long itemId) {
        Long storeId = StoreContext.getCurrentStoreId();
        ArAsset asset =
                arAssetRepository
                        .findByJewelryItemIdAndStoreId(itemId, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("AR asset not found"));

        asset.setActive(false);
        asset.setStatus(ArAsset.ArAssetStatus.DISABLED);
        arAssetRepository.save(asset);

        // Update jewelry item
        JewelryItem item = asset.getJewelryItem();
        item.setArEnabled(false);
        jewelryItemRepository.save(item);
    }

    // ════════════════════════════════════════
    // AR Sessions (Analytics)
    // ════════════════════════════════════════

    @Transactional
    public void logSession(ArSessionRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Long userId = StoreContext.getCurrentUserId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        JewelryItem item =
                jewelryItemRepository
                        .findById(request.getJewelryItemId())
                        .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        ArSession session =
                ArSession.builder()
                        .store(store)
                        .userId(userId)
                        .jewelryItem(item)
                        .durationSeconds(request.getDurationSeconds())
                        .screenshotTaken(
                                request.getScreenshotTaken() != null
                                        && request.getScreenshotTaken())
                        .ledToInvoice(
                                request.getLedToInvoice() != null && request.getLedToInvoice())
                        .invoiceId(request.getInvoiceId())
                        .deviceType(
                                request.getDeviceType() != null
                                        ? request.getDeviceType()
                                        : "DESKTOP_WEBCAM")
                        .build();

        arSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public ArAnalyticsResponse getAnalytics() {
        Long storeId = StoreContext.getCurrentStoreId();
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        long totalSessions = arSessionRepository.countSessionsSince(storeId, thirtyDaysAgo);
        long conversions = arSessionRepository.countConversionsSince(storeId, thirtyDaysAgo);
        long arReady =
                arAssetRepository.countByStoreIdAndStatus(
                        storeId, ArAsset.ArAssetStatus.READY);
        long totalItems = jewelryItemRepository.countByStoreIdAndActiveTrue(storeId);

        List<Object[]> topItems =
                arSessionRepository.findMostTriedItems(storeId, PageRequest.of(0, 10));

        List<ArAnalyticsResponse.TopTriedItem> topList =
                topItems.stream()
                        .map(
                                row -> {
                                    Long itemId = (Long) row[0];
                                    long count = (Long) row[1];
                                    JewelryItem item =
                                            jewelryItemRepository.findById(itemId).orElse(null);
                                    return ArAnalyticsResponse.TopTriedItem.builder()
                                            .itemId(itemId)
                                            .itemName(item != null ? item.getName() : "Unknown")
                                            .itemSku(item != null ? item.getSku() : "")
                                            .tryCount(count)
                                            .build();
                                })
                        .toList();

        return ArAnalyticsResponse.builder()
                .totalSessions(totalSessions)
                .totalConversions(conversions)
                .conversionRate(totalSessions > 0 ? (conversions * 100.0 / totalSessions) : 0)
                .arReadyItems(arReady)
                .totalItems(totalItems)
                .topTriedItems(topList)
                .build();
    }

    // ════════════════════════════════════════
    // Mapper
    // ════════════════════════════════════════

    private ArAssetResponse toResponse(ArAsset asset) {
        JewelryItem item = asset.getJewelryItem();
        return ArAssetResponse.builder()
                .id(asset.getId())
                .jewelryItemId(item.getId())
                .jewelryItemName(item.getName())
                .jewelryItemSku(item.getSku())
                .jewelryItemImageUrl(item.getImageUrl())
                .arType(asset.getArType().name())
                .overlayUrl(asset.getOverlayUrl())
                .modelUrl(asset.getModelUrl())
                .thumbnailUrl(asset.getThumbnailUrl())
                .anchorConfig(asset.getAnchorConfig())
                .status(asset.getStatus().name())
                .source(asset.getSource().name())
                .width(asset.getWidth())
                .height(asset.getHeight())
                .fileSizeBytes(asset.getFileSizeBytes())
                .createdAt(asset.getCreatedAt() != null ? asset.getCreatedAt().toString() : null)
                .build();
    }
}
