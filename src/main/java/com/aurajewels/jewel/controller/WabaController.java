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
package com.aurajewels.jewel.controller;

import com.aurajewels.jewel.dto.messaging.WabaConnectRequest;
import com.aurajewels.jewel.dto.messaging.WabaStatusResponse;
import com.aurajewels.jewel.security.RequiresModule;
import com.aurajewels.jewel.security.RequiresPermission;
import com.aurajewels.jewel.security.StoreContext;
import com.aurajewels.jewel.service.messaging.WabaOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for a store's WhatsApp Business Account connection (Phase-1 manual onboarding).
 *
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/waba")
@RequiredArgsConstructor
public class WabaController {

    private final WabaOnboardingService wabaOnboardingService;

    /** GET /api/waba/status — this store's WhatsApp connection status. */
    @GetMapping("/status")
    @RequiresPermission("MANAGE_SETTINGS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<WabaStatusResponse> status() {
        return ResponseEntity.ok(wabaOnboardingService.getStatus(StoreContext.getCurrentStoreId()));
    }

    /** POST /api/waba/connect — connect (or re-connect) the store's WhatsApp number. */
    @PostMapping("/connect")
    @RequiresPermission("MANAGE_SETTINGS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<WabaStatusResponse> connect(@RequestBody WabaConnectRequest request) {
        return ResponseEntity.ok(
                wabaOnboardingService.connect(StoreContext.getCurrentStoreId(), request));
    }

    /** DELETE /api/waba/disconnect — mark the store's WhatsApp connection disconnected. */
    @DeleteMapping("/disconnect")
    @RequiresPermission("MANAGE_SETTINGS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<WabaStatusResponse> disconnect() {
        return ResponseEntity.ok(
                wabaOnboardingService.disconnect(StoreContext.getCurrentStoreId()));
    }
}
