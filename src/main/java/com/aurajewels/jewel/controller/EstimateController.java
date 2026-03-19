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

import com.aurajewels.jewel.dto.billing.InvoiceRequest;
import com.aurajewels.jewel.dto.billing.InvoiceResponse;
import com.aurajewels.jewel.entity.Estimate;
import com.aurajewels.jewel.security.RequiresModule;
import com.aurajewels.jewel.security.RequiresPermission;
import com.aurajewels.jewel.service.EstimateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/estimates")
@RequiredArgsConstructor
public class EstimateController {

    private final EstimateService estimateService;

    @GetMapping
    @RequiresPermission("VIEW_BILLING")
    @RequiresModule("BILLING")
    public ResponseEntity<List<Estimate>> list() {
        return ResponseEntity.ok(estimateService.listEstimates());
    }

    @GetMapping("/{id}")
    @RequiresPermission("VIEW_BILLING")
    @RequiresModule("BILLING")
    public ResponseEntity<Estimate> get(@PathVariable Long id) {
        return ResponseEntity.ok(estimateService.getEstimate(id));
    }

    @PostMapping
    @RequiresPermission("MANAGE_BILLING")
    @RequiresModule("BILLING")
    public ResponseEntity<Estimate> create(@RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(estimateService.createEstimate(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("MANAGE_BILLING")
    @RequiresModule("BILLING")
    public ResponseEntity<Estimate> update(
            @PathVariable Long id, @RequestBody InvoiceRequest request) {
        return ResponseEntity.ok(estimateService.updateEstimate(id, request));
    }

    @PostMapping("/{id}/convert")
    @RequiresPermission("MANAGE_BILLING")
    @RequiresModule("BILLING")
    public ResponseEntity<InvoiceResponse> convertToInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(estimateService.convertToInvoice(id));
    }
}
