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
import com.aurajewels.jewel.event.InvoiceCreatedEvent;
import com.aurajewels.jewel.repository.*;
import com.aurajewels.jewel.security.StoreContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Invoice service for creating invoices with line items, GST calculation, and payments.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final JewelryItemRepository jewelryItemRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;

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
                                        ? request.getGstAmount()
                                                .divide(
                                                        BigDecimal.valueOf(2),
                                                        2,
                                                        java.math.RoundingMode.HALF_UP)
                                        : BigDecimal.ZERO)
                        .sgstAmount(
                                request.getGstAmount() != null
                                        ? request.getGstAmount()
                                                .divide(
                                                        BigDecimal.valueOf(2),
                                                        2,
                                                        java.math.RoundingMode.HALF_UP)
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

        // Handle invoice items
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (InvoiceItemRequest itemReq : request.getItems()) {
                InvoiceItem item =
                        InvoiceItem.builder()
                                .invoice(invoice)
                                .store(store)
                                .jewelryItemId(itemReq.getJewelryItemId())
                                .quantity(
                                        itemReq.getQuantity() != null && itemReq.getQuantity() > 0
                                                ? itemReq.getQuantity()
                                                : 1)
                                .metalRate(
                                        itemReq.getRate() != null
                                                ? itemReq.getRate()
                                                : BigDecimal.ZERO)
                                .metalValue(
                                        itemReq.getWeight() != null
                                                ? itemReq.getWeight()
                                                : BigDecimal.ZERO)
                                .makingCharges(
                                        itemReq.getMakingCharge() != null
                                                ? itemReq.getMakingCharge()
                                                : BigDecimal.ZERO)
                                .stoneCharges(
                                        itemReq.getStoneCharges() != null
                                                ? itemReq.getStoneCharges()
                                                : BigDecimal.ZERO)
                                .otherCharges(BigDecimal.ZERO)
                                .discount(BigDecimal.ZERO)
                                .taxableAmount(
                                        itemReq.getAmount() != null
                                                ? itemReq.getAmount()
                                                : BigDecimal.ZERO)
                                .cgstPercent(new BigDecimal("1.50"))
                                .sgstPercent(new BigDecimal("1.50"))
                                .cgstAmount(
                                        itemReq.getAmount() != null
                                                ? itemReq.getAmount()
                                                        .multiply(new BigDecimal("0.015"))
                                                        .setScale(2, java.math.RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)
                                .sgstAmount(
                                        itemReq.getAmount() != null
                                                ? itemReq.getAmount()
                                                        .multiply(new BigDecimal("0.015"))
                                                        .setScale(2, java.math.RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)
                                .totalAmount(
                                        itemReq.getAmount() != null
                                                ? itemReq.getAmount()
                                                        .multiply(new BigDecimal("1.03"))
                                                        .setScale(2, java.math.RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO)
                                .build();
                invoice.getItems().add(item);
            }
        }

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

        // Commit stock for a confirmed sale: deduct each line's quantity from inventory.
        if (commitsStock(invoice.getStatus())) {
            adjustStock(invoice, -1);
        }

        String customerName =
                customer.getFirstName()
                        + (customer.getLastName() != null ? " " + customer.getLastName() : "");

        // Post the full, balanced accounting for a confirmed sale (receivable DR + revenue CR +
        // a receipt CR for each payment already recorded). Gated on the same stock-commit rule so
        // drafts post nothing.
        if (commitsStock(invoice.getStatus())) {
            postSaleAccounting(invoice);
        }

        activityLogService.log(
                "Created Invoice",
                "Invoice " + invoiceNumber + " for " + customer.getFirstName(),
                "Billing",
                "INVOICE",
                invoice.getId());

        // Fire-and-forget WhatsApp confirmation (sent after commit, off the request thread).
        eventPublisher.publishEvent(
                new InvoiceCreatedEvent(
                        storeId,
                        customer.getId(),
                        invoice.getId(),
                        invoiceNumber,
                        invoice.getTotalAmount() != null
                                ? invoice.getTotalAmount().toPlainString()
                                : "0",
                        store.getName(),
                        customerName));

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

        Invoice.InvoiceStatus oldStatus = invoice.getStatus();
        Invoice.InvoiceStatus newStatus = Invoice.InvoiceStatus.valueOf(status);
        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);

        // Keep inventory AND accounting in sync with the stock-commit state. Both are driven purely
        // by the transition, so repeat calls are idempotent (e.g. CANCELLED -> CANCELLED is a no-op).
        boolean wasCommitted = commitsStock(oldStatus);
        boolean nowCommitted = commitsStock(newStatus);
        if (wasCommitted && !nowCommitted) {
            adjustStock(invoice, +1); // e.g. CONFIRMED -> CANCELLED: return stock
            reverseInvoiceAccounting(invoice); // post contra entries
        } else if (!wasCommitted && nowCommitted) {
            adjustStock(invoice, -1); // e.g. DRAFT/CANCELLED -> CONFIRMED: commit stock
            reinstateInvoiceAccounting(invoice); // drop any contras, or post fresh
        }

        return toResponse(invoice);
    }

    // ----- Ledger / accounting ------------------------------------------------

    private static final String REF_INVOICE = "INVOICE";
    private static final String REF_INVOICE_REVERSAL = "INVOICE_REVERSAL";
    private static final String CATEGORY_RECEIVABLE = "Receivable";
    private static final String CATEGORY_REVENUE = "Sales";
    private static final String CATEGORY_RECEIPTS = "Receipts";

    /**
     * Post the balanced accounting for a confirmed sale: a receivable DR and a revenue CR for the
     * invoice total, plus a receipt CR for every payment already recorded on the invoice.
     */
    private void postSaleAccounting(Invoice invoice) {
        BigDecimal total =
                invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        LocalDate saleDate =
                invoice.getInvoiceDate() != null ? invoice.getInvoiceDate() : LocalDate.now();
        String mode = invoice.getPaymentMode() != null ? invoice.getPaymentMode().name() : "CASH";
        String num = invoice.getInvoiceNumber();

        ledgerEntryRepository.save(
                buildLedger(
                        invoice,
                        saleDate,
                        LedgerEntry.LedgerType.DR,
                        total,
                        mode,
                        "Invoice " + num + " — receivable",
                        CATEGORY_RECEIVABLE,
                        REF_INVOICE));
        ledgerEntryRepository.save(
                buildLedger(
                        invoice,
                        saleDate,
                        LedgerEntry.LedgerType.CR,
                        total,
                        mode,
                        "Sale — " + num,
                        CATEGORY_REVENUE,
                        REF_INVOICE));
        for (InvoicePayment payment : invoice.getPayments()) {
            ledgerEntryRepository.save(
                    buildLedger(
                            invoice,
                            payment.getPaymentDate() != null
                                    ? payment.getPaymentDate()
                                    : saleDate,
                            LedgerEntry.LedgerType.CR,
                            payment.getAmount(),
                            payment.getMode() != null ? payment.getMode() : "CASH",
                            "Payment for " + num,
                            CATEGORY_RECEIPTS,
                            REF_INVOICE));
        }
    }

    /**
     * Reverse a cancelled invoice's accounting by posting an opposite-type contra for every still
     * active INVOICE entry. Idempotent: does nothing if a reversal already exists.
     */
    private void reverseInvoiceAccounting(Invoice invoice) {
        Long storeId = invoice.getStore().getId();
        String num = invoice.getInvoiceNumber();
        if (ledgerEntryRepository.existsByStoreIdAndReferenceTypeAndReferenceIdAndActiveTrue(
                storeId, REF_INVOICE_REVERSAL, num)) {
            return; // already reversed
        }
        List<LedgerEntry> originals =
                ledgerEntryRepository.findByStoreIdAndReferenceTypeAndReferenceIdAndActiveTrue(
                        storeId, REF_INVOICE, num);
        for (LedgerEntry original : originals) {
            LedgerEntry.LedgerType opposite =
                    original.getType() == LedgerEntry.LedgerType.CR
                            ? LedgerEntry.LedgerType.DR
                            : LedgerEntry.LedgerType.CR;
            ledgerEntryRepository.save(
                    buildLedger(
                            invoice,
                            LocalDate.now(),
                            opposite,
                            original.getAmount(),
                            original.getMode(),
                            "Reversal — cancelled " + num,
                            original.getCategory(),
                            REF_INVOICE_REVERSAL));
        }
    }

    /**
     * Re-activate a previously cancelled invoice's accounting: drop its contra entries if present,
     * otherwise post a fresh sale set (e.g. a draft becoming confirmed). Idempotent.
     */
    private void reinstateInvoiceAccounting(Invoice invoice) {
        Long storeId = invoice.getStore().getId();
        String num = invoice.getInvoiceNumber();
        List<LedgerEntry> reversals =
                ledgerEntryRepository.findByStoreIdAndReferenceTypeAndReferenceIdAndActiveTrue(
                        storeId, REF_INVOICE_REVERSAL, num);
        if (!reversals.isEmpty()) {
            reversals.forEach(entry -> entry.setActive(false));
            ledgerEntryRepository.saveAll(reversals);
            return;
        }
        boolean hasSaleEntries =
                ledgerEntryRepository.existsByStoreIdAndReferenceTypeAndReferenceIdAndActiveTrue(
                        storeId, REF_INVOICE, num);
        if (!hasSaleEntries) {
            postSaleAccounting(invoice);
        }
    }

    private LedgerEntry buildLedger(
            Invoice invoice,
            LocalDate date,
            LedgerEntry.LedgerType type,
            BigDecimal amount,
            String mode,
            String note,
            String category,
            String referenceType) {
        return LedgerEntry.builder()
                .store(invoice.getStore())
                .entryDate(date != null ? date : LocalDate.now())
                .party(partyName(invoice.getCustomer()))
                .type(type)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .mode(mode != null ? mode : "CASH")
                .note(note)
                .category(category)
                .referenceId(invoice.getInvoiceNumber())
                .referenceType(referenceType)
                .createdBy(StoreContext.getCurrentUserId())
                .active(true)
                .build();
    }

    private static String partyName(Customer customer) {
        if (customer == null) {
            return "";
        }
        return customer.getFirstName()
                + (customer.getLastName() != null ? " " + customer.getLastName() : "");
    }

    /** Whether an invoice in this status holds committed (deducted) stock. */
    private static boolean commitsStock(Invoice.InvoiceStatus status) {
        return status == Invoice.InvoiceStatus.CONFIRMED;
    }

    /**
     * Adjust inventory for every line of the invoice within the invoice's own store. {@code sign =
     * -1} deducts the sold quantity (sale), {@code sign = +1} restores it (cancellation). Runs on
     * the item's own store context and writes the item directly, so it never trips the cross-store
     * metal-type/category guard on {@code JewelryItemService.update}.
     */
    private void adjustStock(Invoice invoice, int sign) {
        Long storeId = invoice.getStore().getId();
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getJewelryItemId() == null) {
                continue;
            }
            jewelryItemRepository
                    .findByIdAndStoreId(item.getJewelryItemId(), storeId)
                    .ifPresent(
                            jewelryItem -> {
                                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                                int current =
                                        jewelryItem.getQuantity() != null
                                                ? jewelryItem.getQuantity()
                                                : 0;
                                int updated = Math.max(0, current + sign * qty);
                                jewelryItem.setQuantity(updated);
                                if (updated == 0) {
                                    jewelryItem.setStatus(JewelryItem.ItemStatus.SOLD);
                                } else if (sign > 0) {
                                    jewelryItem.setStatus(JewelryItem.ItemStatus.IN_STOCK);
                                }
                                jewelryItemRepository.save(jewelryItem);
                            });
        }
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

        // Post the receipt (cash-in) for this payment.
        ledgerEntryRepository.save(
                buildLedger(
                        invoice,
                        LocalDate.now(),
                        LedgerEntry.LedgerType.CR,
                        request.getAmount(),
                        request.getMode() != null ? request.getMode() : "CASH",
                        "Payment for " + invoice.getInvoiceNumber(),
                        CATEGORY_RECEIPTS,
                        REF_INVOICE));

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
                                item -> {
                                    String itemName =
                                            item.getJewelryItemId() != null
                                                    ? jewelryItemRepository
                                                            .findById(item.getJewelryItemId())
                                                            .map(JewelryItem::getName)
                                                            .orElse(null)
                                                    : null;
                                    return InvoiceItemResponse.builder()
                                            .id(item.getId())
                                            .jewelryItemId(item.getJewelryItemId())
                                            .name(itemName)
                                            .quantity(item.getQuantity())
                                            .metalRate(item.getMetalRate())
                                            .metalValue(item.getMetalValue())
                                            .makingCharges(item.getMakingCharges())
                                            .stoneCharges(item.getStoneCharges())
                                            .otherCharges(item.getOtherCharges())
                                            .discount(item.getDiscount())
                                            .taxableAmount(item.getTaxableAmount())
                                            .cgstPercent(item.getCgstPercent())
                                            .sgstPercent(item.getSgstPercent())
                                            .cgstAmount(item.getCgstAmount())
                                            .sgstAmount(item.getSgstAmount())
                                            .totalAmount(item.getTotalAmount())
                                            .build();
                                })
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
