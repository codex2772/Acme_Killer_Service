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
package com.aurajewels.jewel.service.messaging;

import com.aurajewels.jewel.event.InvoiceCreatedEvent;
import com.aurajewels.jewel.service.InvoicePdfService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges domain events to the messaging layer. Runs after the source transaction commits (so a
 * rolled-back invoice never messages) and on the messaging executor (so the originating request is
 * never blocked by provider latency).
 *
 * @author Raviraj Bhosale
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessagingEventListener {

    private final MessagingService messagingService;
    private final InvoicePdfService invoicePdfService;

    @Async("messagingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInvoiceCreated(InvoiceCreatedEvent event) {
        Map<String, Object> vars =
                Map.of(
                        "customerName", event.customerName(),
                        "invoiceNo", event.invoiceNumber(),
                        "amount", event.amount(),
                        "storeName", event.storeName());
        try {
            // Preferred path: attach the invoice PDF as the template's DOCUMENT header.
            // A send failure (unreachable URL / unapproved template) is recorded on the
            // message log's failureReason by MessagingService — no exception is thrown here.
            String pdfUrl = invoicePdfService.generateAndUploadPdf(event.invoiceId());
            messagingService.sendTemplated(
                    event.storeId(),
                    event.customerId(),
                    "INVOICE_DOCUMENT",
                    vars,
                    pdfUrl,
                    "INVOICE",
                    event.invoiceId(),
                    null);
        } catch (Exception pdfEx) {
            // PDF generation/upload itself failed — degrade gracefully to the text
            // confirmation so the customer is still notified.
            log.error(
                    "Invoice PDF unavailable for invoice {} ({}); sending text confirmation instead",
                    event.invoiceId(),
                    pdfEx.getMessage(),
                    pdfEx);
            try {
                messagingService.sendTemplated(
                        event.storeId(),
                        event.customerId(),
                        "INVOICE_CONFIRMATION",
                        vars,
                        null,
                        "INVOICE",
                        event.invoiceId(),
                        null);
            } catch (Exception e) {
                log.error(
                        "Failed to send fallback invoice WhatsApp for invoice {}: {}",
                        event.invoiceId(),
                        e.getMessage(),
                        e);
            }
        }
    }
}
