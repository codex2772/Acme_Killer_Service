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

import com.aurajewels.jewel.dto.ar.*;
import com.aurajewels.jewel.security.RequiresPermission;
import com.aurajewels.jewel.service.ArService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AR virtual try-on REST API for managing overlay assets, try-on sessions, and analytics.
 *
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/ar")
@RequiredArgsConstructor
public class ArController {

    private final ArService arService;

    // ── AR Assets ──

    @GetMapping("/assets")
    @RequiresPermission("VIEW_INVENTORY")
    public ResponseEntity<List<ArAssetResponse>> listAssets(
            @RequestParam(required = false) String arType,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(arService.listArAssets(arType, status));
    }

    @GetMapping("/assets/item/{itemId}")
    @RequiresPermission("VIEW_INVENTORY")
    public ResponseEntity<ArAssetResponse> getAssetByItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(arService.getArAsset(itemId));
    }

    @PostMapping("/assets")
    @RequiresPermission("MANAGE_INVENTORY")
    public ResponseEntity<ArAssetResponse> createOrUpdateAsset(
            @RequestBody ArAssetRequest request) {
        return ResponseEntity.ok(arService.createOrUpdateArAsset(request));
    }

    @DeleteMapping("/assets/item/{itemId}")
    @RequiresPermission("MANAGE_INVENTORY")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long itemId) {
        arService.deleteArAsset(itemId);
        return ResponseEntity.noContent().build();
    }

    // ── AR Sessions (Analytics) ──

    @PostMapping("/sessions")
    @RequiresPermission("VIEW_INVENTORY")
    public ResponseEntity<Void> logSession(@RequestBody ArSessionRequest request) {
        arService.logSession(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/analytics")
    @RequiresPermission("VIEW_REPORTS")
    public ResponseEntity<ArAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(arService.getAnalytics());
    }
}
