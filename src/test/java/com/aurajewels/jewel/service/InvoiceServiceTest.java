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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.Invoice;
import com.aurajewels.jewel.entity.InvoiceItem;
import com.aurajewels.jewel.entity.JewelryItem;
import com.aurajewels.jewel.entity.LedgerEntry;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.InvoiceRepository;
import com.aurajewels.jewel.repository.JewelryItemRepository;
import com.aurajewels.jewel.repository.LedgerEntryRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

/** Verifies that cancelling / un-cancelling an invoice keeps inventory in sync, atomically. */
class InvoiceServiceTest {

    private static final Long STORE_ID = 3L;
    private static final Long INVOICE_ID = 1L;
    private static final Long JEWELRY_ID = 100L;
    private static final int SOLD_QTY = 3;

    private InvoiceRepository invoiceRepository;
    private JewelryItemRepository jewelryItemRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private InvoiceService service;
    private Invoice invoice;
    private JewelryItem jewelryItem;

    /** In-memory ledger so the mocked repo behaves like a real store. */
    private final List<LedgerEntry> ledger = new ArrayList<>();

    @BeforeEach
    void setUp() {
        invoiceRepository = Mockito.mock(InvoiceRepository.class);
        CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        jewelryItemRepository = Mockito.mock(JewelryItemRepository.class);
        ledgerEntryRepository = Mockito.mock(LedgerEntryRepository.class);
        ActivityLogService activityLogService = Mockito.mock(ActivityLogService.class);
        ApplicationEventPublisher eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

        service =
                new InvoiceService(
                        invoiceRepository,
                        customerRepository,
                        storeRepository,
                        jewelryItemRepository,
                        ledgerEntryRepository,
                        activityLogService,
                        eventPublisher);

        Store store = new Store();
        store.setId(STORE_ID);
        Customer customer = new Customer();
        customer.setId(9L);
        customer.setFirstName("Test");

        // Item was sold out by the sale: quantity 0, status SOLD.
        jewelryItem =
                JewelryItem.builder()
                        .name("22K Gold Necklace")
                        .quantity(0)
                        .status(JewelryItem.ItemStatus.SOLD)
                        .build();
        jewelryItem.setId(JEWELRY_ID);

        InvoiceItem line =
                InvoiceItem.builder().jewelryItemId(JEWELRY_ID).quantity(SOLD_QTY).build();
        line.setId(50L);

        invoice =
                Invoice.builder()
                        .store(store)
                        .customer(customer)
                        .invoiceNumber("INV-1")
                        .status(Invoice.InvoiceStatus.CONFIRMED)
                        .items(new ArrayList<>(List.of(line)))
                        .payments(new ArrayList<>())
                        .build();
        invoice.setId(INVOICE_ID);

        StoreContext ctx = new StoreContext();
        ctx.setStoreId(STORE_ID);
        StoreContext.set(ctx);

        when(invoiceRepository.findByIdAndStoreId(INVOICE_ID, STORE_ID))
                .thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Store-scoped lookup used by the restore/deduct path.
        when(jewelryItemRepository.findByIdAndStoreId(JEWELRY_ID, STORE_ID))
                .thenReturn(Optional.of(jewelryItem));
        when(jewelryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Used only by toResponse for the item name.
        when(jewelryItemRepository.findById(JEWELRY_ID)).thenReturn(Optional.of(jewelryItem));

        // Stateful ledger repo.
        when(ledgerEntryRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            LedgerEntry e = inv.getArgument(0);
                            if (e.getId() == null) {
                                e.setId((long) (ledger.size() + 1));
                                ledger.add(e);
                            }
                            return e;
                        });
        when(ledgerEntryRepository.saveAll(any()))
                .thenAnswer(inv -> inv.getArgument(0)); // entries are already in `ledger`
        when(ledgerEntryRepository
                        .findByStoreIdAndReferenceTypeAndReferenceIdAndActiveTrue(
                                any(), any(), any()))
                .thenAnswer(
                        inv ->
                                ledger.stream()
                                        .filter(e -> Boolean.TRUE.equals(e.getActive()))
                                        .filter(e -> e.getReferenceType().equals(inv.getArgument(1)))
                                        .filter(e -> e.getReferenceId().equals(inv.getArgument(2)))
                                        .toList());
        when(ledgerEntryRepository
                        .existsByStoreIdAndReferenceTypeAndReferenceIdAndActiveTrue(
                                any(), any(), any()))
                .thenAnswer(
                        inv ->
                                ledger.stream()
                                        .filter(e -> Boolean.TRUE.equals(e.getActive()))
                                        .filter(e -> e.getReferenceType().equals(inv.getArgument(1)))
                                        .anyMatch(
                                                e -> e.getReferenceId().equals(inv.getArgument(2))));
    }

    /** Seed the balanced sale entries a create would have posted (receivable DR + revenue CR). */
    private void seedSaleLedger() {
        ledger.add(
                LedgerEntry.builder()
                        .store(invoice.getStore())
                        .type(LedgerEntry.LedgerType.DR)
                        .amount(new BigDecimal("1000.00"))
                        .referenceType("INVOICE")
                        .referenceId("INV-1")
                        .category("Receivable")
                        .mode("CASH")
                        .active(true)
                        .build());
        ledger.add(
                LedgerEntry.builder()
                        .store(invoice.getStore())
                        .type(LedgerEntry.LedgerType.CR)
                        .amount(new BigDecimal("1000.00"))
                        .referenceType("INVOICE")
                        .referenceId("INV-1")
                        .category("Sales")
                        .mode("CASH")
                        .active(true)
                        .build());
    }

    private BigDecimal activeNet() {
        BigDecimal net = BigDecimal.ZERO;
        for (LedgerEntry e : ledger) {
            if (!Boolean.TRUE.equals(e.getActive())) {
                continue;
            }
            net =
                    e.getType() == LedgerEntry.LedgerType.CR
                            ? net.add(e.getAmount())
                            : net.subtract(e.getAmount());
        }
        return net;
    }

    private long activeReversals() {
        return ledger.stream()
                .filter(e -> Boolean.TRUE.equals(e.getActive()))
                .filter(e -> "INVOICE_REVERSAL".equals(e.getReferenceType()))
                .count();
    }

    @AfterEach
    void tearDown() {
        StoreContext.clear();
    }

    @Test
    void cancellingRestoresExactSoldQuantityAndMarksInStock() {
        service.updateStatus(INVOICE_ID, "CANCELLED");

        assertThat(jewelryItem.getQuantity()).isEqualTo(SOLD_QTY); // 0 + 3, not +1
        assertThat(jewelryItem.getStatus()).isEqualTo(JewelryItem.ItemStatus.IN_STOCK);
        assertThat(invoice.getStatus()).isEqualTo(Invoice.InvoiceStatus.CANCELLED);
    }

    @Test
    void cancellingTwiceDoesNotDoubleRestore() {
        service.updateStatus(INVOICE_ID, "CANCELLED");
        service.updateStatus(INVOICE_ID, "CANCELLED");

        assertThat(jewelryItem.getQuantity()).isEqualTo(SOLD_QTY); // still 3, not 6
    }

    @Test
    void reconfirmingAfterCancelReDeductsStock() {
        service.updateStatus(INVOICE_ID, "CANCELLED"); // 0 -> 3, IN_STOCK
        service.updateStatus(INVOICE_ID, "CONFIRMED"); // 3 -> 0, SOLD

        assertThat(jewelryItem.getQuantity()).isZero();
        assertThat(jewelryItem.getStatus()).isEqualTo(JewelryItem.ItemStatus.SOLD);
    }

    @Test
    void cancellingPostsContraEntriesThatNetToZero() {
        seedSaleLedger();
        assertThat(activeNet()).isEqualByComparingTo("0.00"); // DR 1000 + CR 1000

        service.updateStatus(INVOICE_ID, "CANCELLED");

        assertThat(activeReversals()).isEqualTo(2); // one contra per sale entry
        assertThat(activeNet()).isEqualByComparingTo("0.00"); // sale + reversal all net to zero
    }

    @Test
    void cancellingTwiceDoesNotDoubleReverse() {
        seedSaleLedger();
        service.updateStatus(INVOICE_ID, "CANCELLED");
        service.updateStatus(INVOICE_ID, "CANCELLED");

        assertThat(activeReversals()).isEqualTo(2); // still 2, not 4
    }

    @Test
    void reconfirmingDropsTheReversalEntries() {
        seedSaleLedger();
        service.updateStatus(INVOICE_ID, "CANCELLED");
        service.updateStatus(INVOICE_ID, "CONFIRMED");

        assertThat(activeReversals()).isZero(); // contras deactivated on un-cancel
    }
}
