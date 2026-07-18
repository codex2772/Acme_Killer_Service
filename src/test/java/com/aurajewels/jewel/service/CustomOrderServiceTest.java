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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aurajewels.jewel.dto.billing.InvoiceResponse;
import com.aurajewels.jewel.dto.customorder.AdvancePaymentRequest;
import com.aurajewels.jewel.dto.customorder.CustomOrderItemRequest;
import com.aurajewels.jewel.dto.customorder.CustomOrderRequest;
import com.aurajewels.jewel.dto.customorder.CustomOrderResponse;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.Organization;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.OrganizationRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for the custom order lifecycle: create → advance → deliver → convert to invoice.
 *
 * @author Raviraj Bhosale
 */
@SpringBootTest
@ActiveProfiles("test")
class CustomOrderServiceTest {

    @Autowired private CustomOrderService customOrderService;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private CustomerRepository customerRepository;

    private Long storeId;
    private Long customerId;

    @BeforeEach
    void setUp() {
        Organization org =
                organizationRepository.save(Organization.builder().name("Test Org").build());
        Store store =
                storeRepository.save(Store.builder().organization(org).name("Test Store").build());
        storeId = store.getId();

        // Unique phone per run to satisfy the unique constraint.
        Customer customer =
                customerRepository.save(
                        Customer.builder()
                                .store(store)
                                .firstName("Priya")
                                .lastName("Sharma")
                                .phone("9" + System.nanoTime() % 1_000_000_000L)
                                .build());
        customerId = customer.getId();

        StoreContext ctx = new StoreContext();
        ctx.setStoreId(storeId);
        ctx.setUserId(1L);
        ctx.setOrgId(org.getId());
        ctx.setRole("OWNER");
        StoreContext.set(ctx);
    }

    @AfterEach
    void tearDown() {
        StoreContext.clear();
    }

    @Test
    void createAdvanceAndConvertToInvoice() {
        // 1. Create a bespoke custom order (item not in catalog — jewelryItemId is null).
        CustomOrderRequest request = new CustomOrderRequest();
        request.setCustomerId(customerId);
        request.setSubtotal(new BigDecimal("50000.00"));
        request.setGstAmount(new BigDecimal("1500.00"));
        request.setTotal(new BigDecimal("51500.00"));
        request.setExpectedDeliveryDate(LocalDate.now().plusDays(10));
        request.setGoldsmithName("Ramesh");

        CustomOrderItemRequest item = new CustomOrderItemRequest();
        item.setName("Custom gold ring, 22K");
        item.setWeight(new BigDecimal("8.000"));
        item.setPurity("22K");
        item.setAmount(new BigDecimal("50000.00"));
        request.setItems(List.of(item));

        CustomOrderResponse created = customOrderService.createOrder(request);

        assertThat(created.getOrderNumber()).isEqualTo("CO-00001");
        assertThat(created.getStatus()).isEqualTo("PENDING");
        assertThat(created.getPaymentStatus()).isEqualTo("UNPAID");
        assertThat(created.getBalanceAmount()).isEqualByComparingTo("51500.00");
        assertThat(created.getItems()).hasSize(1);

        // 2. Record a partial advance.
        AdvancePaymentRequest advance = new AdvancePaymentRequest();
        advance.setMode("CASH");
        advance.setAmount(new BigDecimal("10000.00"));
        CustomOrderResponse afterAdvance =
                customOrderService.recordAdvance(created.getId(), advance);

        assertThat(afterAdvance.getPaymentStatus()).isEqualTo("PARTIAL");
        assertThat(afterAdvance.getPaidAmount()).isEqualByComparingTo("10000.00");
        assertThat(afterAdvance.getBalanceAmount()).isEqualByComparingTo("41500.00");

        // 3. Move through the production lifecycle.
        customOrderService.updateStatus(created.getId(), "IN_PROGRESS");
        CustomOrderResponse ready = customOrderService.updateStatus(created.getId(), "READY");
        assertThat(ready.getStatus()).isEqualTo("READY");

        // 4. Convert to invoice (the final bill).
        InvoiceResponse invoice = customOrderService.convertToInvoice(created.getId());

        assertThat(invoice.getInvoiceNumber()).startsWith("INV-");
        assertThat(invoice.getTotal()).isEqualByComparingTo("51500.00");
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo("10000.00");
        assertThat(invoice.getPaymentStatus()).isEqualTo("PARTIAL");

        CustomOrderResponse delivered = customOrderService.getOrder(created.getId());
        assertThat(delivered.getStatus()).isEqualTo("DELIVERED");
        assertThat(delivered.getConvertedInvoiceId()).isEqualTo(invoice.getId());
        assertThat(delivered.getDeliveredDate()).isNotNull();

        // 5. Re-converting is rejected.
        assertThatThrownBy(() -> customOrderService.convertToInvoice(created.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already converted");
    }

    @Test
    void orderNumbersAreSequentialPerStore() {
        CustomOrderResponse first = customOrderService.createOrder(minimalOrder());
        CustomOrderResponse second = customOrderService.createOrder(minimalOrder());
        assertThat(first.getOrderNumber()).isEqualTo("CO-00001");
        assertThat(second.getOrderNumber()).isEqualTo("CO-00002");
    }

    @Test
    void totalsAreRecomputedServerSideFromItems() {
        // Client sends bogus header totals; the server must ignore them and derive from the item.
        CustomOrderRequest request = orderWithItem(new BigDecimal("50000.00"));
        request.setSubtotal(new BigDecimal("1.00"));
        request.setGstAmount(new BigDecimal("1.00"));
        request.setTotal(new BigDecimal("1.00"));

        CustomOrderResponse created = customOrderService.createOrder(request);

        assertThat(created.getSubtotal()).isEqualByComparingTo("50000.00");
        assertThat(created.getGstAmount()).isEqualByComparingTo("1500.00"); // 3% default
        assertThat(created.getEstimatedTotal()).isEqualByComparingTo("51500.00");
    }

    @Test
    void balancePaymentSettlesTheOrder() {
        CustomOrderResponse order =
                customOrderService.createOrder(orderWithItem(new BigDecimal("50000.00")));

        customOrderService.recordAdvance(order.getId(), payment(new BigDecimal("10000.00")));
        CustomOrderResponse settled =
                customOrderService.recordBalance(
                        order.getId(), payment(new BigDecimal("41500.00")));

        assertThat(settled.getPaymentStatus()).isEqualTo("PAID");
        assertThat(settled.getBalanceAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void overpaymentIsRejected() {
        CustomOrderResponse order =
                customOrderService.createOrder(orderWithItem(new BigDecimal("50000.00")));

        assertThatThrownBy(
                        () ->
                                customOrderService.recordAdvance(
                                        order.getId(), payment(new BigDecimal("60000.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void cancelSoftDeletesAndHidesFromList() {
        CustomOrderResponse order =
                customOrderService.createOrder(orderWithItem(new BigDecimal("50000.00")));

        CustomOrderResponse cancelled = customOrderService.cancelOrder(order.getId());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");

        // Soft-deleted: no longer in the active list, but still fetchable by id.
        assertThat(customOrderService.listOrders(null, null, null, null))
                .noneMatch(o -> o.getId().equals(order.getId()));
        assertThat(customOrderService.getOrder(order.getId()).getStatus()).isEqualTo("CANCELLED");

        // No payments against a cancelled order.
        assertThatThrownBy(
                        () ->
                                customOrderService.recordAdvance(
                                        order.getId(), payment(new BigDecimal("100.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void invalidStatusTransitionsAreRejected() {
        CustomOrderResponse order =
                customOrderService.createOrder(orderWithItem(new BigDecimal("50000.00")));

        // CANCELLED must go through the cancel endpoint.
        assertThatThrownBy(() -> customOrderService.updateStatus(order.getId(), "CANCELLED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cancel endpoint");

        // Unknown status value.
        assertThatThrownBy(() -> customOrderService.updateStatus(order.getId(), "FLYING"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid order status");

        // Terminal state: DELIVERED cannot move back.
        customOrderService.updateStatus(order.getId(), "DELIVERED");
        assertThatThrownBy(() -> customOrderService.updateStatus(order.getId(), "IN_PROGRESS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot change status");
    }

    private CustomOrderRequest minimalOrder() {
        CustomOrderRequest request = new CustomOrderRequest();
        request.setCustomerId(customerId);
        request.setTotal(new BigDecimal("1000.00"));
        return request;
    }

    private CustomOrderRequest orderWithItem(BigDecimal itemAmount) {
        CustomOrderRequest request = new CustomOrderRequest();
        request.setCustomerId(customerId);

        CustomOrderItemRequest item = new CustomOrderItemRequest();
        item.setName("Custom gold ring, 22K");
        item.setWeight(new BigDecimal("8.000"));
        item.setPurity("22K");
        item.setAmount(itemAmount);
        request.setItems(List.of(item));
        return request;
    }

    private AdvancePaymentRequest payment(BigDecimal amount) {
        AdvancePaymentRequest req = new AdvancePaymentRequest();
        req.setMode("CASH");
        req.setAmount(amount);
        return req;
    }
}
