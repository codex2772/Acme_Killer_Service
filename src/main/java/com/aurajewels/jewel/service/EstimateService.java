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

import com.aurajewels.jewel.dto.billing.*;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.*;
import com.aurajewels.jewel.security.StoreContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class EstimateService {

    private final EstimateRepository estimateRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final InvoiceService invoiceService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<Estimate> listEstimates() {
        Long storeId = StoreContext.getCurrentStoreId();
        return estimateRepository.findByStoreIdAndActiveTrueOrderByCreatedAtDesc(storeId);
    }

    @Transactional(readOnly = true)
    public Estimate getEstimate(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return estimateRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Estimate not found"));
    }

    @Transactional
    public Estimate createEstimate(InvoiceRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Customer customer =
                customerRepository
                        .findById(request.getCustomerId())
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        String prefix = "EST-";
        int nextNum = estimateRepository.findMaxEstimateNumber(storeId, prefix) + 1;
        String estimateNumber = prefix + String.format("%05d", nextNum);

        Estimate estimate =
                Estimate.builder()
                        .store(store)
                        .estimateNumber(estimateNumber)
                        .customer(customer)
                        .estimateDate(
                                request.getDate() != null ? request.getDate() : LocalDate.now())
                        .subtotal(
                                request.getSubtotal() != null
                                        ? request.getSubtotal()
                                        : BigDecimal.ZERO)
                        .discount(
                                request.getDiscount() != null
                                        ? request.getDiscount()
                                        : BigDecimal.ZERO)
                        .gstRate(
                                request.getGstRate() != null
                                        ? request.getGstRate()
                                        : new BigDecimal("3.00"))
                        .gstAmount(
                                request.getGstAmount() != null
                                        ? request.getGstAmount()
                                        : BigDecimal.ZERO)
                        .roundOff(
                                request.getRoundOff() != null
                                        ? request.getRoundOff()
                                        : BigDecimal.ZERO)
                        .totalAmount(
                                request.getTotal() != null ? request.getTotal() : BigDecimal.ZERO)
                        .notes(request.getNotes())
                        .digitalSignature(request.getDigitalSignature())
                        .status(Estimate.EstimateStatus.DRAFT)
                        .active(true)
                        .createdBy(StoreContext.getCurrentUserId())
                        .items(new ArrayList<>())
                        .build();

        // Add line items
        if (request.getItems() != null) {
            for (InvoiceItemRequest itemReq : request.getItems()) {
                EstimateItem item =
                        EstimateItem.builder()
                                .estimate(estimate)
                                .store(store)
                                .jewelryItemId(itemReq.getJewelryItemId())
                                .name(itemReq.getName())
                                .weight(itemReq.getWeight())
                                .purity(itemReq.getPurity())
                                .rate(itemReq.getRate())
                                .makingCharge(itemReq.getMakingCharge())
                                .makingChargeType(
                                        itemReq.getMakingChargeType() != null
                                                ? EstimateItem.MakingChargeType.valueOf(
                                                        itemReq.getMakingChargeType())
                                                : EstimateItem.MakingChargeType.PERCENTAGE)
                                .wastage(itemReq.getWastage())
                                .amount(itemReq.getAmount())
                                .build();
                estimate.getItems().add(item);
            }
        }

        estimate = estimateRepository.save(estimate);

        activityLogService.log(
                "Created Estimate",
                "Estimate " + estimateNumber + " for " + customer.getFirstName(),
                "Billing",
                "ESTIMATE",
                estimate.getId());

        return estimate;
    }

    @Transactional
    public Estimate updateEstimate(Long id, InvoiceRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Estimate estimate =
                estimateRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Estimate not found"));

        if (request.getNotes() != null) estimate.setNotes(request.getNotes());
        if (request.getSubtotal() != null) estimate.setSubtotal(request.getSubtotal());
        if (request.getDiscount() != null) estimate.setDiscount(request.getDiscount());
        if (request.getGstRate() != null) estimate.setGstRate(request.getGstRate());
        if (request.getGstAmount() != null) estimate.setGstAmount(request.getGstAmount());
        if (request.getRoundOff() != null) estimate.setRoundOff(request.getRoundOff());
        if (request.getTotal() != null) estimate.setTotalAmount(request.getTotal());
        if (request.getDigitalSignature() != null)
            estimate.setDigitalSignature(request.getDigitalSignature());

        // Replace items
        if (request.getItems() != null) {
            estimate.getItems().clear();
            for (InvoiceItemRequest itemReq : request.getItems()) {
                EstimateItem item =
                        EstimateItem.builder()
                                .estimate(estimate)
                                .store(store)
                                .jewelryItemId(itemReq.getJewelryItemId())
                                .name(itemReq.getName())
                                .weight(itemReq.getWeight())
                                .purity(itemReq.getPurity())
                                .rate(itemReq.getRate())
                                .makingCharge(itemReq.getMakingCharge())
                                .makingChargeType(
                                        itemReq.getMakingChargeType() != null
                                                ? EstimateItem.MakingChargeType.valueOf(
                                                        itemReq.getMakingChargeType())
                                                : EstimateItem.MakingChargeType.PERCENTAGE)
                                .wastage(itemReq.getWastage())
                                .amount(itemReq.getAmount())
                                .build();
                estimate.getItems().add(item);
            }
        }

        return estimateRepository.save(estimate);
    }

    @Transactional
    public InvoiceResponse convertToInvoice(Long estimateId) {
        Long storeId = StoreContext.getCurrentStoreId();
        Estimate estimate =
                estimateRepository
                        .findByIdAndStoreId(estimateId, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Estimate not found"));

        if (estimate.getStatus() == Estimate.EstimateStatus.CONVERTED) {
            throw new IllegalArgumentException("Estimate already converted");
        }

        // Create invoice from estimate
        InvoiceRequest invoiceRequest = new InvoiceRequest();
        invoiceRequest.setCustomerId(estimate.getCustomer().getId());
        invoiceRequest.setDate(LocalDate.now());
        invoiceRequest.setSubtotal(estimate.getSubtotal());
        invoiceRequest.setDiscount(estimate.getDiscount());
        invoiceRequest.setGstRate(estimate.getGstRate());
        invoiceRequest.setGstAmount(estimate.getGstAmount());
        invoiceRequest.setRoundOff(estimate.getRoundOff());
        invoiceRequest.setTotal(estimate.getTotalAmount());
        invoiceRequest.setNotes(estimate.getNotes());
        invoiceRequest.setDigitalSignature(estimate.getDigitalSignature());

        InvoiceResponse invoiceResponse = invoiceService.createInvoice(invoiceRequest);

        estimate.setStatus(Estimate.EstimateStatus.CONVERTED);
        estimate.setConvertedInvoiceId(invoiceResponse.getId());
        estimateRepository.save(estimate);

        activityLogService.log(
                "Converted Estimate to Invoice",
                "Estimate "
                        + estimate.getEstimateNumber()
                        + " → Invoice "
                        + invoiceResponse.getInvoiceNumber(),
                "Billing",
                "ESTIMATE",
                estimate.getId());

        return invoiceResponse;
    }
}
