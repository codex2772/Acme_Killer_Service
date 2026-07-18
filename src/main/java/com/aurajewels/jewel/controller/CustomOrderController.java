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

import com.aurajewels.jewel.dto.billing.InvoiceResponse;
import com.aurajewels.jewel.dto.billing.StatusUpdateRequest;
import com.aurajewels.jewel.dto.customorder.AdvancePaymentRequest;
import com.aurajewels.jewel.dto.customorder.CustomOrderRequest;
import com.aurajewels.jewel.dto.customorder.CustomOrderResponse;
import com.aurajewels.jewel.security.RequiresModule;
import com.aurajewels.jewel.security.RequiresPermission;
import com.aurajewels.jewel.service.CustomOrderService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for bespoke custom orders: create, track advances and production lifecycle, and generate
 * the final bill by converting the order into an Invoice.
 *
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/custom-orders")
@RequiredArgsConstructor
public class CustomOrderController {

    private final CustomOrderService customOrderService;

    @GetMapping
    @RequiresPermission("VIEW_CUSTOM_ORDERS")
    @RequiresModule("CUSTOM_ORDERS")
    public ResponseEntity<List<CustomOrderResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        return ResponseEntity.ok(customOrderService.listOrders(status, paymentStatus, from, to));
    }

    @GetMapping("/{id}")
    @RequiresPermission("VIEW_CUSTOM_ORDERS")
    @RequiresModule("CUSTOM_ORDERS")
    public ResponseEntity<CustomOrderResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(customOrderService.getOrder(id));
    }

    @PostMapping
    @RequiresPermission("MANAGE_CUSTOM_ORDERS")
    @RequiresModule("CUSTOM_ORDERS")
    public ResponseEntity<CustomOrderResponse> create(
            @Valid @RequestBody CustomOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customOrderService.createOrder(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("MANAGE_CUSTOM_ORDERS")
    @RequiresModule("CUSTOM_ORDERS")
    public ResponseEntity<CustomOrderResponse> update(
            @PathVariable Long id, @Valid @RequestBody CustomOrderRequest request) {
        return ResponseEntity.ok(customOrderService.updateOrder(id, request));
    }

    @PatchMapping("/{id}/status")
    @RequiresPermission("MANAGE_CUSTOM_ORDERS")
    @RequiresModule("CUSTOM_ORDERS")
    public ResponseEntity<CustomOrderResponse> updateStatus(
            @PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(customOrderService.updateStatus(id, request.getStatus()));
    }

    @PostMapping("/{id}/advance")
    @RequiresPermission("MANAGE_CUSTOM_ORDERS")
    @RequiresModule("CUSTOM_ORDERS")
    public ResponseEntity<CustomOrderResponse> recordAdvance(
            @PathVariable Long id, @Valid @RequestBody AdvancePaymentRequest request) {
        return ResponseEntity.ok(customOrderService.recordAdvance(id, request));
    }

    @PostMapping("/{id}/balance")
    @RequiresPermission("MANAGE_CUSTOM_ORDERS")
    @RequiresModule("CUSTOM_ORDERS")
    public ResponseEntity<CustomOrderResponse> recordBalance(
            @PathVariable Long id, @Valid @RequestBody AdvancePaymentRequest request) {
        return ResponseEntity.ok(customOrderService.recordBalance(id, request));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("MANAGE_CUSTOM_ORDERS")
    @RequiresModule("CUSTOM_ORDERS")
    public ResponseEntity<CustomOrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(customOrderService.cancelOrder(id));
    }

    @PostMapping("/{id}/convert")
    @RequiresPermission("MANAGE_CUSTOM_ORDERS")
    @RequiresModule("CUSTOM_ORDERS")
    public ResponseEntity<InvoiceResponse> convert(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customOrderService.convertToInvoice(id));
    }
}
