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

import com.aurajewels.jewel.dto.customerapp.*;
import com.aurajewels.jewel.security.StoreContext;
import com.aurajewels.jewel.service.CustomerAppService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the customer-facing mobile application. Provides registration, login, catalog
 * browsing, wishlist, enquiry, and profile management.
 *
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/customer-app")
@RequiredArgsConstructor
public class CustomerAppController {

    private final CustomerAppService customerAppService;

    // ======================== AUTH (Public) ========================

    @PostMapping("/register")
    public ResponseEntity<CustomerLoginResponse> register(
            @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerAppService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<CustomerLoginResponse> login(@RequestBody CustomerLoginRequest request) {
        return ResponseEntity.ok(customerAppService.login(request));
    }

    // ======================== CATALOG (Public) ========================

    @GetMapping("/catalog/{storeId}")
    public ResponseEntity<List<CatalogItemResponse>> getCatalog(@PathVariable Long storeId) {
        // If customer is logged in, pass their ID for wishlist status
        Long customerId = getCustomerIdOrNull();
        return ResponseEntity.ok(customerAppService.getCatalog(storeId, customerId));
    }

    @GetMapping("/catalog/{storeId}/items/{itemId}")
    public ResponseEntity<CatalogItemResponse> getCatalogItem(
            @PathVariable Long storeId, @PathVariable Long itemId) {
        Long customerId = getCustomerIdOrNull();
        return ResponseEntity.ok(customerAppService.getCatalogItem(storeId, itemId, customerId));
    }

    // ======================== WISHLIST (Authenticated) ========================

    @GetMapping("/wishlist")
    public ResponseEntity<List<CatalogItemResponse>> getWishlist() {
        Long customerId = requireCustomerId();
        return ResponseEntity.ok(customerAppService.getWishlist(customerId));
    }

    @PostMapping("/wishlist/{jewelryItemId}")
    public ResponseEntity<Map<String, String>> addToWishlist(@PathVariable Long jewelryItemId) {
        Long customerId = requireCustomerId();
        customerAppService.addToWishlist(customerId, jewelryItemId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Item added to wishlist"));
    }

    @DeleteMapping("/wishlist/{jewelryItemId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long jewelryItemId) {
        Long customerId = requireCustomerId();
        customerAppService.removeFromWishlist(customerId, jewelryItemId);
        return ResponseEntity.noContent().build();
    }

    // ======================== ENQUIRY (Authenticated) ========================

    @PostMapping("/enquiry")
    public ResponseEntity<EnquiryResponse> createEnquiry(@RequestBody EnquiryRequest request) {
        Long customerId = requireCustomerId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerAppService.createEnquiry(customerId, request));
    }

    @GetMapping("/enquiries")
    public ResponseEntity<List<EnquiryResponse>> getEnquiries() {
        Long customerId = requireCustomerId();
        return ResponseEntity.ok(customerAppService.getEnquiries(customerId));
    }

    // ======================== PROFILE (Authenticated) ========================

    @GetMapping("/profile")
    public ResponseEntity<CustomerProfileResponse> getProfile() {
        Long customerId = requireCustomerId();
        return ResponseEntity.ok(customerAppService.getProfile(customerId));
    }

    @PutMapping("/profile")
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            @RequestBody CustomerProfileUpdateRequest request) {
        Long customerId = requireCustomerId();
        return ResponseEntity.ok(customerAppService.updateProfile(customerId, request));
    }

    // ======================== HELPERS ========================

    /**
     * Extracts customer ID from the JWT context. For CUSTOMER role tokens, the userId in
     * StoreContext IS the customer ID.
     */
    private Long requireCustomerId() {
        Long userId = StoreContext.getCurrentUserId();
        String role = StoreContext.getCurrentRole();
        if (userId == null || !"CUSTOMER".equals(role)) {
            throw new IllegalArgumentException("Customer authentication required");
        }
        return userId;
    }

    /** Returns customer ID if logged in, null otherwise (for public endpoints like catalog). */
    private Long getCustomerIdOrNull() {
        try {
            String role = StoreContext.getCurrentRole();
            if ("CUSTOMER".equals(role)) {
                return StoreContext.getCurrentUserId();
            }
        } catch (Exception ignored) {
            // Not logged in — that's fine for public catalog
        }
        return null;
    }
}
