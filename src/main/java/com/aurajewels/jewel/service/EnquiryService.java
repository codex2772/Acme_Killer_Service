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

import com.aurajewels.jewel.dto.enquiry.AdminEnquiryResponse;
import com.aurajewels.jewel.dto.enquiry.EnquiryReplyRequest;
import com.aurajewels.jewel.entity.CustomerEnquiry;
import com.aurajewels.jewel.repository.CustomerEnquiryRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin-side service for managing customer enquiries. Allows owner/admin/staff to view all
 * enquiries for their store and respond to them.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class EnquiryService {

    private final CustomerEnquiryRepository enquiryRepository;

    /** List all enquiries for the current store (newest first). */
    @Transactional(readOnly = true)
    public List<AdminEnquiryResponse> listEnquiries() {
        Long storeId = StoreContext.getCurrentStoreId();
        List<CustomerEnquiry> enquiries = enquiryRepository.findByStoreIdWithDetails(storeId);
        return enquiries.stream().map(this::toAdminResponse).toList();
    }

    /** Get a single enquiry by ID. */
    @Transactional(readOnly = true)
    public AdminEnquiryResponse getEnquiry(Long id) {
        CustomerEnquiry enquiry =
                enquiryRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Enquiry not found: " + id));
        return toAdminResponse(enquiry);
    }

    /** Respond to a customer enquiry. */
    @Transactional
    public AdminEnquiryResponse respondToEnquiry(Long id, EnquiryReplyRequest request) {
        CustomerEnquiry enquiry =
                enquiryRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Enquiry not found: " + id));

        if (request.getAdminResponse() != null && !request.getAdminResponse().isBlank()) {
            enquiry.setAdminResponse(request.getAdminResponse());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            enquiry.setStatus(CustomerEnquiry.EnquiryStatus.valueOf(request.getStatus()));
        } else {
            // Default to RESPONDED when admin sends a reply
            enquiry.setStatus(CustomerEnquiry.EnquiryStatus.RESPONDED);
        }

        enquiry.setRespondedBy(StoreContext.getCurrentUserId());
        enquiry.setRespondedAt(Instant.now());

        enquiryRepository.save(enquiry);

        return toAdminResponse(enquiry);
    }

    /** Close an enquiry without further response. */
    @Transactional
    public AdminEnquiryResponse closeEnquiry(Long id) {
        CustomerEnquiry enquiry =
                enquiryRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Enquiry not found: " + id));

        enquiry.setStatus(CustomerEnquiry.EnquiryStatus.CLOSED);
        enquiry.setRespondedBy(StoreContext.getCurrentUserId());
        enquiry.setRespondedAt(Instant.now());

        enquiryRepository.save(enquiry);

        return toAdminResponse(enquiry);
    }

    /** Map entity to admin response DTO with customer contact details. */
    private AdminEnquiryResponse toAdminResponse(CustomerEnquiry enquiry) {
        return AdminEnquiryResponse.builder()
                .id(enquiry.getId())
                .customerId(enquiry.getCustomer().getId())
                .customerName(
                        buildCustomerName(
                                enquiry.getCustomer().getFirstName(),
                                enquiry.getCustomer().getLastName()))
                .customerPhone(enquiry.getCustomer().getPhone())
                .customerEmail(enquiry.getCustomer().getEmail())
                .jewelryItemId(
                        enquiry.getJewelryItem() != null ? enquiry.getJewelryItem().getId() : null)
                .jewelryItemName(
                        enquiry.getJewelryItem() != null
                                ? enquiry.getJewelryItem().getName()
                                : null)
                .jewelryItemSku(
                        enquiry.getJewelryItem() != null ? enquiry.getJewelryItem().getSku() : null)
                .subject(enquiry.getSubject())
                .message(enquiry.getMessage())
                .imageUrl(enquiry.getImageUrl())
                .status(enquiry.getStatus().name())
                .adminResponse(enquiry.getAdminResponse())
                .respondedBy(enquiry.getRespondedBy())
                .respondedAt(enquiry.getRespondedAt())
                .createdAt(enquiry.getCreatedAt())
                .updatedAt(enquiry.getUpdatedAt())
                .build();
    }

    /** Build full customer name from first + last name. */
    private String buildCustomerName(String firstName, String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
