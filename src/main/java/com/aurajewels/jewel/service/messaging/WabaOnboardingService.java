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
package com.aurajewels.jewel.service.messaging;

import com.aurajewels.jewel.dto.messaging.WabaConnectRequest;
import com.aurajewels.jewel.dto.messaging.WabaStatusResponse;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.entity.StoreWaba;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.repository.StoreWabaRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages a store's WhatsApp Business Account connection. Phase-1 onboarding takes the Meta Cloud
 * API credentials directly (owner-pasted); embedded signup replaces this later. The access token is
 * handed to the {@link WabaTokenStore} and only its reference is persisted.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WabaOnboardingService {

    private final StoreWabaRepository storeWabaRepository;
    private final StoreRepository storeRepository;
    private final WabaTokenStore tokenStore;

    @Transactional
    public WabaStatusResponse connect(Long storeId, WabaConnectRequest request) {
        if (request.getPhoneNumberId() == null || request.getPhoneNumberId().isBlank()) {
            throw new IllegalArgumentException("phoneNumberId is required");
        }
        if (request.getAccessToken() == null || request.getAccessToken().isBlank()) {
            throw new IllegalArgumentException("accessToken is required");
        }

        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Store not found: " + storeId));

        StoreWaba waba = storeWabaRepository.findByStore_Id(storeId).orElseGet(StoreWaba::new);
        waba.setStore(store);

        String tokenRef = tokenStore.store(storeId, request.getAccessToken());

        waba.setPhoneNumberId(request.getPhoneNumberId());
        waba.setWabaId(request.getWabaId());
        waba.setDisplayName(request.getDisplayName());
        waba.setDisplayNumber(request.getDisplayNumber());
        waba.setAccessTokenRef(tokenRef);
        waba.setProvider("META");
        waba.setStatus(StoreWaba.WabaStatus.CONNECTED);
        if (waba.getQualityRating() == null) {
            waba.setQualityRating(StoreWaba.QualityRating.UNKNOWN);
        }
        waba.setConnectedAt(Instant.now());
        waba.setActive(true);

        waba = storeWabaRepository.save(waba);
        log.info("Store {} connected WhatsApp number {}", storeId, request.getDisplayNumber());
        return toStatus(waba);
    }

    @Transactional(readOnly = true)
    public WabaStatusResponse getStatus(Long storeId) {
        return storeWabaRepository
                .findByStore_Id(storeId)
                .map(this::toStatus)
                .orElseGet(
                        () ->
                                WabaStatusResponse.builder()
                                        .connected(false)
                                        .status("NOT_CONNECTED")
                                        .build());
    }

    @Transactional
    public WabaStatusResponse disconnect(Long storeId) {
        StoreWaba waba =
                storeWabaRepository
                        .findByStore_Id(storeId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No WhatsApp connection for store"));
        waba.setStatus(StoreWaba.WabaStatus.DISCONNECTED);
        waba = storeWabaRepository.save(waba);
        log.info("Store {} disconnected WhatsApp", storeId);
        return toStatus(waba);
    }

    private WabaStatusResponse toStatus(StoreWaba waba) {
        return WabaStatusResponse.builder()
                .connected(waba.getStatus() == StoreWaba.WabaStatus.CONNECTED)
                .status(waba.getStatus() != null ? waba.getStatus().name() : null)
                .displayName(waba.getDisplayName())
                .displayNumber(waba.getDisplayNumber())
                .qualityRating(
                        waba.getQualityRating() != null ? waba.getQualityRating().name() : null)
                .messagingTier(waba.getMessagingTier())
                .build();
    }
}
