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

import com.aurajewels.jewel.dto.enquiry.AdminEnquiryResponse;
import com.aurajewels.jewel.dto.enquiry.EnquiryReplyRequest;
import com.aurajewels.jewel.security.RequiresModule;
import com.aurajewels.jewel.security.RequiresPermission;
import com.aurajewels.jewel.service.EnquiryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-side controller for managing customer enquiries. Owner/Admin/Staff can view enquiries
 * submitted by customers, see their contact info, respond, and close.
 *
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/enquiries")
@RequiredArgsConstructor
public class EnquiryController {

    private final EnquiryService enquiryService;

    /** GET /api/enquiries — List all enquiries for the current store. */
    @GetMapping
    @RequiresPermission("VIEW_CUSTOMERS")
    @RequiresModule("CUSTOMERS")
    public ResponseEntity<List<AdminEnquiryResponse>> list() {
        return ResponseEntity.ok(enquiryService.listEnquiries());
    }

    /** GET /api/enquiries/{id} — Get a single enquiry with full details. */
    @GetMapping("/{id}")
    @RequiresPermission("VIEW_CUSTOMERS")
    @RequiresModule("CUSTOMERS")
    public ResponseEntity<AdminEnquiryResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryService.getEnquiry(id));
    }

    /** PUT /api/enquiries/{id}/respond — Respond to a customer enquiry. */
    @PutMapping("/{id}/respond")
    @RequiresPermission("MANAGE_CUSTOMERS")
    @RequiresModule("CUSTOMERS")
    public ResponseEntity<AdminEnquiryResponse> respond(
            @PathVariable Long id, @RequestBody EnquiryReplyRequest request) {
        return ResponseEntity.ok(enquiryService.respondToEnquiry(id, request));
    }

    /** PATCH /api/enquiries/{id}/close — Close an enquiry. */
    @PatchMapping("/{id}/close")
    @RequiresPermission("MANAGE_CUSTOMERS")
    @RequiresModule("CUSTOMERS")
    public ResponseEntity<AdminEnquiryResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryService.closeEnquiry(id));
    }
}
