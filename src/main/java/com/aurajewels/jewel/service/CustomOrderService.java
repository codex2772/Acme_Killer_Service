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

import com.aurajewels.jewel.dto.billing.InvoiceItemRequest;
import com.aurajewels.jewel.dto.billing.InvoiceRequest;
import com.aurajewels.jewel.dto.billing.InvoiceResponse;
import com.aurajewels.jewel.dto.billing.SplitPaymentRequest;
import com.aurajewels.jewel.dto.customorder.*;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.CustomOrderRepository;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for bespoke custom orders: creation, advance/balance tracking, production lifecycle, and
 * conversion into a final Invoice (the bill) on delivery.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class CustomOrderService {

    private static final String ORDER_PREFIX = "CO-";

    /**
     * Allowed production status transitions. CANCELLED is intentionally absent — cancellation goes
     * through {@link #cancelOrder(Long)} so the soft-delete {@code active} flag stays consistent.
     * DELIVERED and CANCELLED are terminal.
     */
    private static final Map<CustomOrder.OrderStatus, Set<CustomOrder.OrderStatus>>
            ALLOWED_TRANSITIONS = new EnumMap<>(CustomOrder.OrderStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(
                CustomOrder.OrderStatus.PENDING,
                Set.of(
                        CustomOrder.OrderStatus.IN_PROGRESS,
                        CustomOrder.OrderStatus.READY,
                        CustomOrder.OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(
                CustomOrder.OrderStatus.IN_PROGRESS,
                Set.of(
                        CustomOrder.OrderStatus.PENDING,
                        CustomOrder.OrderStatus.READY,
                        CustomOrder.OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(
                CustomOrder.OrderStatus.READY,
                Set.of(CustomOrder.OrderStatus.IN_PROGRESS, CustomOrder.OrderStatus.DELIVERED));
        ALLOWED_TRANSITIONS.put(CustomOrder.OrderStatus.DELIVERED, Set.of());
        ALLOWED_TRANSITIONS.put(CustomOrder.OrderStatus.CANCELLED, Set.of());
    }

    private final CustomOrderRepository customOrderRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final InvoiceService invoiceService;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<CustomOrderResponse> listOrders(
            String status, String paymentStatus, LocalDate from, LocalDate to) {
        Long storeId = StoreContext.getCurrentStoreId();
        CustomOrder.OrderStatus orderStatus = status != null ? parseStatus(status) : null;
        CustomOrder.PaymentStatus payStatus =
                paymentStatus != null ? parsePaymentStatus(paymentStatus) : null;
        return customOrderRepository
                .findFiltered(storeId, orderStatus, payStatus, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomOrderResponse getOrder(Long id) {
        return toResponse(loadOrder(id));
    }

    @Transactional
    public CustomOrderResponse createOrder(CustomOrderRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        Customer customer =
                customerRepository
                        .findById(request.getCustomerId())
                        .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        int nextNum = customOrderRepository.findMaxOrderNumber(storeId, ORDER_PREFIX) + 1;
        String orderNumber = ORDER_PREFIX + String.format("%05d", nextNum);

        CustomOrder order =
                CustomOrder.builder()
                        .store(store)
                        .orderNumber(orderNumber)
                        .customer(customer)
                        .orderDate(request.getDate() != null ? request.getDate() : LocalDate.now())
                        .subtotal(BigDecimal.ZERO)
                        .discount(nz(request.getDiscount()))
                        .gstRate(
                                request.getGstRate() != null
                                        ? request.getGstRate()
                                        : new BigDecimal("3.00"))
                        .gstAmount(BigDecimal.ZERO)
                        .roundOff(nz(request.getRoundOff()))
                        .totalAmount(BigDecimal.ZERO)
                        .paidAmount(BigDecimal.ZERO)
                        .paymentStatus(CustomOrder.PaymentStatus.UNPAID)
                        .status(
                                request.getStatus() != null
                                        ? parseStatus(request.getStatus())
                                        : CustomOrder.OrderStatus.PENDING)
                        .expectedDeliveryDate(request.getExpectedDeliveryDate())
                        .goldsmithName(request.getGoldsmithName())
                        .assignedTo(request.getAssignedTo())
                        .designNotes(request.getDesignNotes())
                        .referenceImageUrl(request.getReferenceImageUrl())
                        .notes(request.getNotes())
                        .digitalSignature(request.getDigitalSignature())
                        .active(true)
                        .createdBy(StoreContext.getCurrentUserId())
                        .items(new ArrayList<>())
                        .payments(new ArrayList<>())
                        .build();

        applyItems(order, store, request.getItems());
        recomputeTotals(order);

        order = customOrderRepository.save(order);

        activityLogService.log(
                "Created Custom Order",
                "Custom Order " + orderNumber + " for " + customer.getFirstName(),
                "Custom Orders",
                "CUSTOM_ORDER",
                order.getId());

        return toResponse(order);
    }

    @Transactional
    public CustomOrderResponse updateOrder(Long id, CustomOrderRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        CustomOrder order = loadOrder(id);

        if (order.getConvertedInvoiceId() != null) {
            throw new IllegalArgumentException("Order already converted to an invoice");
        }

        // subtotal, gstAmount and totalAmount are recomputed server-side (see recomputeTotals);
        // only the inputs to that calculation are accepted from the client.
        if (request.getDiscount() != null) order.setDiscount(request.getDiscount());
        if (request.getGstRate() != null) order.setGstRate(request.getGstRate());
        if (request.getRoundOff() != null) order.setRoundOff(request.getRoundOff());
        if (request.getExpectedDeliveryDate() != null)
            order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        if (request.getGoldsmithName() != null) order.setGoldsmithName(request.getGoldsmithName());
        if (request.getAssignedTo() != null) order.setAssignedTo(request.getAssignedTo());
        if (request.getDesignNotes() != null) order.setDesignNotes(request.getDesignNotes());
        if (request.getReferenceImageUrl() != null)
            order.setReferenceImageUrl(request.getReferenceImageUrl());
        if (request.getNotes() != null) order.setNotes(request.getNotes());
        if (request.getDigitalSignature() != null)
            order.setDigitalSignature(request.getDigitalSignature());

        // Replace items if a new set is supplied
        if (request.getItems() != null) {
            order.getItems().clear();
            applyItems(order, store, request.getItems());
        }

        recomputeTotals(order);
        recomputePaymentStatus(order);
        customOrderRepository.save(order);
        return toResponse(order);
    }

    @Transactional
    public CustomOrderResponse updateStatus(Long id, String status) {
        CustomOrder order = loadOrder(id);
        CustomOrder.OrderStatus newStatus = parseStatus(status);
        CustomOrder.OrderStatus current = order.getStatus();

        if (order.getConvertedInvoiceId() != null) {
            throw new IllegalArgumentException(
                    "Order already converted to an invoice; its status is locked");
        }
        if (newStatus == CustomOrder.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Use the cancel endpoint to cancel an order");
        }
        if (newStatus != current
                && !ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Cannot change status from " + current + " to " + newStatus);
        }

        order.setStatus(newStatus);
        if (newStatus == CustomOrder.OrderStatus.DELIVERED && order.getDeliveredDate() == null) {
            order.setDeliveredDate(LocalDate.now());
        }
        customOrderRepository.save(order);

        activityLogService.log(
                "Updated Custom Order Status",
                "Custom Order " + order.getOrderNumber() + " → " + newStatus,
                "Custom Orders",
                "CUSTOM_ORDER",
                order.getId());
        return toResponse(order);
    }

    @Transactional
    public CustomOrderResponse recordAdvance(Long id, AdvancePaymentRequest request) {
        return recordPayment(id, request, CustomOrderPayment.PaymentKind.ADVANCE);
    }

    @Transactional
    public CustomOrderResponse recordBalance(Long id, AdvancePaymentRequest request) {
        return recordPayment(id, request, CustomOrderPayment.PaymentKind.BALANCE);
    }

    private CustomOrderResponse recordPayment(
            Long id, AdvancePaymentRequest request, CustomOrderPayment.PaymentKind kind) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        CustomOrder order = loadOrder(id);

        if (order.getConvertedInvoiceId() != null) {
            throw new IllegalArgumentException(
                    "Order already converted; collect the balance on the invoice instead");
        }
        if (order.getStatus() == CustomOrder.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot record a payment against a cancelled order");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        BigDecimal newPaid = nz(order.getPaidAmount()).add(request.getAmount());
        if (newPaid.compareTo(nz(order.getTotalAmount())) > 0) {
            throw new IllegalArgumentException(
                    "Payment exceeds the order balance of "
                            + nz(order.getTotalAmount()).subtract(nz(order.getPaidAmount())));
        }

        CustomOrderPayment payment =
                CustomOrderPayment.builder()
                        .customOrder(order)
                        .store(store)
                        .kind(kind)
                        .mode(request.getMode() != null ? request.getMode() : "CASH")
                        .amount(request.getAmount())
                        .reference(request.getReference())
                        .paymentDate(LocalDate.now())
                        .build();
        order.getPayments().add(payment);
        order.setPaidAmount(newPaid);
        recomputePaymentStatus(order);

        customOrderRepository.save(order);

        activityLogService.log(
                kind == CustomOrderPayment.PaymentKind.ADVANCE
                        ? "Custom Order Advance"
                        : "Custom Order Balance Payment",
                (kind == CustomOrderPayment.PaymentKind.ADVANCE ? "Advance of " : "Balance of ")
                        + request.getAmount()
                        + " via "
                        + payment.getMode()
                        + " for Custom Order "
                        + order.getOrderNumber(),
                "Custom Orders",
                "CUSTOM_ORDER",
                order.getId());
        return toResponse(order);
    }

    /**
     * Cancels an order and soft-deletes it (so it drops out of the active list). Blocked once the
     * order has been converted into an invoice.
     */
    @Transactional
    public CustomOrderResponse cancelOrder(Long id) {
        CustomOrder order = loadOrder(id);
        if (order.getConvertedInvoiceId() != null) {
            throw new IllegalArgumentException(
                    "Cannot cancel an order already converted to an invoice");
        }
        if (order.getStatus() == CustomOrder.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Order is already cancelled");
        }

        order.setStatus(CustomOrder.OrderStatus.CANCELLED);
        order.setActive(false);
        customOrderRepository.save(order);

        activityLogService.log(
                "Cancelled Custom Order",
                "Custom Order " + order.getOrderNumber() + " cancelled",
                "Custom Orders",
                "CUSTOM_ORDER",
                order.getId());
        return toResponse(order);
    }

    /**
     * Generates the final bill by converting the custom order into a regular Invoice. The advance
     * already collected is carried across as the invoice's payments so the invoice balance is
     * correct, and the sales ledger is written once (by {@link InvoiceService#createInvoice}) at
     * delivery — the GST supply date.
     */
    @Transactional
    public InvoiceResponse convertToInvoice(Long id) {
        CustomOrder order = loadOrder(id);

        if (order.getConvertedInvoiceId() != null) {
            throw new IllegalArgumentException("Order already converted to an invoice");
        }
        if (order.getStatus() == CustomOrder.OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot convert a cancelled order");
        }

        InvoiceRequest invoiceRequest = new InvoiceRequest();
        invoiceRequest.setCustomerId(order.getCustomer().getId());
        invoiceRequest.setDate(LocalDate.now());
        invoiceRequest.setSubtotal(order.getSubtotal());
        invoiceRequest.setDiscount(order.getDiscount());
        invoiceRequest.setGstRate(order.getGstRate());
        invoiceRequest.setGstAmount(order.getGstAmount());
        invoiceRequest.setRoundOff(order.getRoundOff());
        invoiceRequest.setTotal(order.getTotalAmount());
        invoiceRequest.setNotes(
                "Custom Order "
                        + order.getOrderNumber()
                        + (order.getNotes() != null ? " — " + order.getNotes() : ""));
        invoiceRequest.setDigitalSignature(order.getDigitalSignature());

        List<InvoiceItemRequest> itemRequests = new ArrayList<>();
        for (CustomOrderItem item : order.getItems()) {
            InvoiceItemRequest ir = new InvoiceItemRequest();
            ir.setJewelryItemId(item.getJewelryItemId());
            ir.setName(item.getName());
            ir.setWeight(item.getWeight());
            ir.setPurity(item.getPurity());
            ir.setRate(item.getRate());
            ir.setMakingCharge(item.getMakingCharge());
            ir.setMakingChargeType(
                    item.getMakingChargeType() != null ? item.getMakingChargeType().name() : null);
            ir.setWastage(item.getWastage());
            ir.setStoneCharges(item.getStoneCharges());
            ir.setAmount(item.getAmount());
            itemRequests.add(ir);
        }
        invoiceRequest.setItems(itemRequests);

        // Carry each advance across as an invoice payment so the invoice balance is correct.
        List<SplitPaymentRequest> splits = new ArrayList<>();
        for (CustomOrderPayment p : order.getPayments()) {
            SplitPaymentRequest sp = new SplitPaymentRequest();
            sp.setMode(p.getMode());
            sp.setAmount(p.getAmount());
            sp.setReference(p.getReference());
            splits.add(sp);
        }
        invoiceRequest.setSplitPayments(splits);

        InvoiceResponse invoiceResponse = invoiceService.createInvoice(invoiceRequest);

        order.setConvertedInvoiceId(invoiceResponse.getId());
        order.setStatus(CustomOrder.OrderStatus.DELIVERED);
        if (order.getDeliveredDate() == null) {
            order.setDeliveredDate(LocalDate.now());
        }
        customOrderRepository.save(order);

        activityLogService.log(
                "Converted Custom Order to Invoice",
                "Custom Order "
                        + order.getOrderNumber()
                        + " → Invoice "
                        + invoiceResponse.getInvoiceNumber(),
                "Custom Orders",
                "CUSTOM_ORDER",
                order.getId());

        return invoiceResponse;
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private CustomOrder loadOrder(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return customOrderRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Custom order not found"));
    }

    private void applyItems(CustomOrder order, Store store, List<CustomOrderItemRequest> items) {
        if (items == null) return;
        for (CustomOrderItemRequest itemReq : items) {
            CustomOrderItem item =
                    CustomOrderItem.builder()
                            .customOrder(order)
                            .store(store)
                            .jewelryItemId(itemReq.getJewelryItemId())
                            .name(itemReq.getName())
                            .weight(itemReq.getWeight())
                            .purity(itemReq.getPurity())
                            .rate(nz(itemReq.getRate()))
                            .makingCharge(nz(itemReq.getMakingCharge()))
                            .makingChargeType(
                                    itemReq.getMakingChargeType() != null
                                            ? CustomOrderItem.MakingChargeType.valueOf(
                                                    itemReq.getMakingChargeType())
                                            : CustomOrderItem.MakingChargeType.PERCENTAGE)
                            .wastage(nz(itemReq.getWastage()))
                            .stoneCharges(nz(itemReq.getStoneCharges()))
                            .amount(nz(itemReq.getAmount()))
                            .build();
            order.getItems().add(item);
        }
    }

    /**
     * Recomputes the order header from its line items so the stored totals are always internally
     * consistent and cannot be spoofed by the client: {@code subtotal} is the sum of line amounts,
     * {@code gstAmount} is derived from the taxable base (subtotal − discount) and {@code gstRate},
     * and {@code totalAmount = subtotal − discount + gstAmount + roundOff}. The per-line {@code
     * amount} (computed against the live metal rate) is trusted, mirroring the invoice flow.
     */
    private void recomputeTotals(CustomOrder order) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CustomOrderItem item : order.getItems()) {
            subtotal = subtotal.add(nz(item.getAmount()));
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = nz(order.getDiscount());
        BigDecimal taxableBase = subtotal.subtract(discount);
        if (taxableBase.compareTo(BigDecimal.ZERO) < 0) {
            taxableBase = BigDecimal.ZERO;
        }
        BigDecimal gstAmount =
                taxableBase
                        .multiply(nz(order.getGstRate()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = taxableBase.add(gstAmount).add(nz(order.getRoundOff()));

        order.setSubtotal(subtotal);
        order.setGstAmount(gstAmount);
        order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));
    }

    private static CustomOrder.OrderStatus parseStatus(String status) {
        try {
            return CustomOrder.OrderStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid order status: " + status);
        }
    }

    private static CustomOrder.PaymentStatus parsePaymentStatus(String status) {
        try {
            return CustomOrder.PaymentStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid payment status: " + status);
        }
    }

    private void recomputePaymentStatus(CustomOrder order) {
        BigDecimal paid = nz(order.getPaidAmount());
        BigDecimal total = nz(order.getTotalAmount());
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            order.setPaymentStatus(CustomOrder.PaymentStatus.UNPAID);
        } else if (paid.compareTo(total) >= 0) {
            order.setPaymentStatus(CustomOrder.PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(CustomOrder.PaymentStatus.PARTIAL);
        }
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private CustomOrderResponse toResponse(CustomOrder order) {
        List<CustomOrderItemResponse> itemResponses =
                order.getItems().stream()
                        .map(
                                item ->
                                        CustomOrderItemResponse.builder()
                                                .id(item.getId())
                                                .jewelryItemId(item.getJewelryItemId())
                                                .name(item.getName())
                                                .weight(item.getWeight())
                                                .purity(item.getPurity())
                                                .rate(item.getRate())
                                                .makingCharge(item.getMakingCharge())
                                                .makingChargeType(
                                                        item.getMakingChargeType() != null
                                                                ? item.getMakingChargeType().name()
                                                                : null)
                                                .wastage(item.getWastage())
                                                .stoneCharges(item.getStoneCharges())
                                                .amount(item.getAmount())
                                                .build())
                        .toList();

        List<CustomOrderPaymentResponse> paymentResponses =
                order.getPayments().stream()
                        .map(
                                p ->
                                        CustomOrderPaymentResponse.builder()
                                                .id(p.getId())
                                                .kind(
                                                        p.getKind() != null
                                                                ? p.getKind().name()
                                                                : null)
                                                .mode(p.getMode())
                                                .amount(p.getAmount())
                                                .reference(p.getReference())
                                                .date(p.getPaymentDate())
                                                .build())
                        .toList();

        BigDecimal balance = nz(order.getTotalAmount()).subtract(nz(order.getPaidAmount()));
        if (balance.compareTo(BigDecimal.ZERO) < 0) balance = BigDecimal.ZERO;

        return CustomOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer().getId())
                .customer(
                        order.getCustomer().getFirstName()
                                + (order.getCustomer().getLastName() != null
                                        ? " " + order.getCustomer().getLastName()
                                        : ""))
                .storeId(order.getStore().getId())
                .date(order.getOrderDate())
                .items(itemResponses)
                .subtotal(order.getSubtotal())
                .gstRate(order.getGstRate())
                .gstAmount(order.getGstAmount())
                .discount(order.getDiscount())
                .roundOff(order.getRoundOff())
                .estimatedTotal(order.getTotalAmount())
                .paidAmount(order.getPaidAmount())
                .balanceAmount(balance)
                .paymentStatus(
                        order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .payments(paymentResponses)
                .status(order.getStatus() != null ? order.getStatus().name() : null)
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .deliveredDate(order.getDeliveredDate())
                .goldsmithName(order.getGoldsmithName())
                .assignedTo(order.getAssignedTo())
                .designNotes(order.getDesignNotes())
                .referenceImageUrl(order.getReferenceImageUrl())
                .notes(order.getNotes())
                .digitalSignature(order.getDigitalSignature())
                .convertedInvoiceId(order.getConvertedInvoiceId())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
