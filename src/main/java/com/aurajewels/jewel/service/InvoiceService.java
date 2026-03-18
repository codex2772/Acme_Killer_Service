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
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listInvoices(
            String status, String paymentStatus, LocalDate from, LocalDate to) {
        Long storeId = StoreContext.getCurrentStoreId();
        Invoice.InvoiceStatus invStatus =
                status != null ? Invoice.InvoiceStatus.valueOf(status) : null;
        Invoice.PaymentStatus payStatus =
                paymentStatus != null ? Invoice.PaymentStatus.valueOf(paymentStatus) : null;
        List<Invoice> invoices =
                invoiceRepository.findFiltered(storeId, invStatus, payStatus, from, to);
        return invoices.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        Invoice invoice =
                invoiceRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Customer customer =
                customerRepository
                        .findById(request.getCustomerId())
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        String prefix = "INV-";
        int nextNum = invoiceRepository.findMaxInvoiceNumber(storeId, prefix) + 1;
        String invoiceNumber = prefix + String.format("%05d", nextNum);

        Invoice invoice =
                Invoice.builder()
                        .store(store)
                        .invoiceType(Invoice.InvoiceType.INVOICE)
                        .invoiceNumber(invoiceNumber)
                        .customer(customer)
                        .invoiceDate(
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
                        .cgstAmount(
                            request.getGstAmount() != null
                                ? request.getGstAmount().divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .sgstAmount(
                            request.getGstAmount() != null
                                ? request.getGstAmount().divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .igstAmount(BigDecimal.ZERO)
                        .roundOff(
                                request.getRoundOff() != null
                                        ? request.getRoundOff()
                                        : BigDecimal.ZERO)
                        .totalAmount(
                                request.getTotal() != null ? request.getTotal() : BigDecimal.ZERO)
                        .oldGoldAdjustment(
                                request.getOldGoldAdjustment() != null
                                        ? request.getOldGoldAdjustment()
                                        : BigDecimal.ZERO)
                        .paymentMode(
                                request.getPaymentMode() != null
                                        ? Invoice.PaymentMode.valueOf(request.getPaymentMode())
                                        : null)
                        .dueDate(request.getDueDate())
                        .notes(request.getNotes())
                        .digitalSignature(request.getDigitalSignature())
                        .status(
                                request.getStatus() != null
                                        ? Invoice.InvoiceStatus.valueOf(request.getStatus())
                                        : Invoice.InvoiceStatus.CONFIRMED)
                        .paymentStatus(Invoice.PaymentStatus.UNPAID)
                        .paidAmount(BigDecimal.ZERO)
                        .active(true)
                        .createdBy(StoreContext.getCurrentUserId())
                        .items(new ArrayList<>())
                        .payments(new ArrayList<>())
                        .build();

        invoiceRepository.save(invoice);

        // Handle split payments
        if (request.getSplitPayments() != null && !request.getSplitPayments().isEmpty()) {
            BigDecimal totalPaid = BigDecimal.ZERO;
            for (SplitPaymentRequest sp : request.getSplitPayments()) {
                InvoicePayment payment =
                        InvoicePayment.builder()
                                .invoice(invoice)
                                .store(store)
                                .mode(sp.getMode())
                                .amount(sp.getAmount())
                                .reference(sp.getReference())
                                .paymentDate(
                                        request.getDate() != null
                                                ? request.getDate()
                                                : LocalDate.now())
                                .build();
                invoice.getPayments().add(payment);
                totalPaid = totalPaid.add(sp.getAmount());
            }
            invoice.setPaidAmount(totalPaid);
            if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setPaymentStatus(Invoice.PaymentStatus.PAID);
            } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                invoice.setPaymentStatus(Invoice.PaymentStatus.PARTIAL);
            }
        }

        invoiceRepository.save(invoice);

        activityLogService.log(
                "Created Invoice",
                "Invoice " + invoiceNumber + " for " + customer.getFirstName(),
                "Billing",
                "INVOICE",
                invoice.getId());

        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse updateInvoice(Long id, InvoiceRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Invoice invoice =
                invoiceRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if (request.getNotes() != null) invoice.setNotes(request.getNotes());
        if (request.getDiscount() != null) invoice.setDiscount(request.getDiscount());
        if (request.getSubtotal() != null) invoice.setSubtotal(request.getSubtotal());
        if (request.getGstRate() != null) invoice.setGstRate(request.getGstRate());
        if (request.getGstAmount() != null) invoice.setGstAmount(request.getGstAmount());
        if (request.getRoundOff() != null) invoice.setRoundOff(request.getRoundOff());
        if (request.getTotal() != null) invoice.setTotalAmount(request.getTotal());
        if (request.getOldGoldAdjustment() != null)
            invoice.setOldGoldAdjustment(request.getOldGoldAdjustment());
        if (request.getDueDate() != null) invoice.setDueDate(request.getDueDate());
        if (request.getDigitalSignature() != null)
            invoice.setDigitalSignature(request.getDigitalSignature());

        invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse updateStatus(Long id, String status) {
        Long storeId = StoreContext.getCurrentStoreId();
        Invoice invoice =
                invoiceRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        invoice.setStatus(Invoice.InvoiceStatus.valueOf(status));
        invoiceRepository.save(invoice);
        return toResponse(invoice);
    }

    @Transactional
    public InvoiceResponse recordPayment(Long invoiceId, SplitPaymentRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Invoice invoice =
                invoiceRepository
                        .findByIdAndStoreId(invoiceId, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        InvoicePayment payment =
                InvoicePayment.builder()
                        .invoice(invoice)
                        .store(store)
                        .mode(request.getMode())
                        .amount(request.getAmount())
                        .reference(request.getReference())
                        .paymentDate(LocalDate.now())
                        .build();
        invoice.getPayments().add(payment);

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(request.getAmount());
        invoice.setPaidAmount(newPaidAmount);

        if (newPaidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setPaymentStatus(Invoice.PaymentStatus.PAID);
        } else {
            invoice.setPaymentStatus(Invoice.PaymentStatus.PARTIAL);
        }

        invoiceRepository.save(invoice);

        activityLogService.log(
                "Payment Recorded",
                "Payment of "
                        + request.getAmount()
                        + " via "
                        + request.getMode()
                        + " for Invoice "
                        + invoice.getInvoiceNumber(),
                "Billing",
                "INVOICE",
                invoice.getId());

        return toResponse(invoice);
    }

    private InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceItemResponse> itemResponses =
                invoice.getItems().stream()
                        .map(
                                item ->
                                        InvoiceItemResponse.builder()
                                                .id(item.getId())
                                                .jewelryItemId(item.getJewelryItemId())
                                                .name(null)
                                                .weight(item.getMetalValue())
                                                .purity(null)
                                                .rate(item.getMetalRate())
                                                .makingCharge(item.getMakingCharges())
                                                .makingChargeType(null)
                                                .wastage(null)
                                                .amount(item.getTotalAmount())
                                                .build())
                        .toList();
        List<PaymentResponse> paymentResponses =
                invoice.getPayments().stream()
                        .map(
                                p ->
                                        PaymentResponse.builder()
                                                .id(p.getId())
                                                .mode(p.getMode())
                                                .amount(p.getAmount())
                                                .reference(p.getReference())
                                                .date(p.getPaymentDate())
                                                .build())
                        .toList();

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerId(invoice.getCustomer().getId())
                .customer(
                        invoice.getCustomer().getFirstName()
                                + (invoice.getCustomer().getLastName() != null
                                        ? " " + invoice.getCustomer().getLastName()
                                        : ""))
                .storeId(invoice.getStore().getId())
                .type(
                        invoice.getInvoiceType() != null
                                ? invoice.getInvoiceType().name()
                                : "INVOICE")
                .date(invoice.getInvoiceDate())
                .items(itemResponses)
                .subtotal(invoice.getSubtotal())
                .gstRate(invoice.getGstRate())
                .gstAmount(invoice.getGstAmount())
                .discount(invoice.getDiscount())
                .roundOff(invoice.getRoundOff())
                .total(invoice.getTotalAmount())
                .oldGoldAdjustment(invoice.getOldGoldAdjustment())
                .paymentMode(
                        invoice.getPaymentMode() != null ? invoice.getPaymentMode().name() : null)
                .splitPayments(paymentResponses)
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                .paymentStatus(
                        invoice.getPaymentStatus() != null
                                ? invoice.getPaymentStatus().name()
                                : null)
                .paidAmount(invoice.getPaidAmount())
                .notes(invoice.getNotes())
                .digitalSignature(invoice.getDigitalSignature())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
