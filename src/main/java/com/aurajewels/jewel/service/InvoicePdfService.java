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

import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.Invoice;
import com.aurajewels.jewel.entity.InvoiceItem;
import com.aurajewels.jewel.entity.JewelryItem;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.InvoiceRepository;
import com.aurajewels.jewel.repository.JewelryItemRepository;
import com.aurajewels.jewel.service.InvoicePdfGenerator.PdfLine;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders an invoice to a PDF bill and hosts it on S3, returning a public HTTPS URL that Meta can
 * fetch as the document header of the WhatsApp invoice template.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private static final int WIDTH = 78; // Courier chars per line at 10pt on A4

    private final InvoiceRepository invoiceRepository;
    private final JewelryItemRepository jewelryItemRepository;
    private final InvoicePdfGenerator pdfGenerator;
    private final S3Service s3Service;

    /**
     * Generate the invoice PDF and upload it to S3.
     *
     * @param invoiceId the invoice to render
     * @return public HTTPS URL of the hosted PDF
     */
    @Transactional(readOnly = true)
    public String generateAndUploadPdf(Long invoiceId) {
        Invoice invoice =
                invoiceRepository
                        .findById(invoiceId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Invoice not found: " + invoiceId));
        byte[] pdf = pdfGenerator.generate(buildLines(invoice));
        String url = s3Service.uploadBytes(pdf, "application/pdf", "invoices", ".pdf");
        log.info("Generated invoice PDF for {} -> {}", invoice.getInvoiceNumber(), url);
        return url;
    }

    private List<PdfLine> buildLines(Invoice invoice) {
        Store store = invoice.getStore();
        Customer customer = invoice.getCustomer();
        List<InvoiceItem> items = invoice.getItems();
        Map<Long, String> itemNames = itemNames(items);

        List<PdfLine> lines = new ArrayList<>();
        lines.add(PdfLine.bold(nz(store.getName(), "Jewellery Store"), 16));
        if (isSet(store.getAddress())) {
            lines.add(PdfLine.of(store.getAddress()));
        }
        StringBuilder storeMeta = new StringBuilder();
        if (isSet(store.getPhone())) {
            storeMeta.append("Phone: ").append(store.getPhone());
        }
        if (isSet(store.getGstin())) {
            if (storeMeta.length() > 0) {
                storeMeta.append("    ");
            }
            storeMeta.append("GSTIN: ").append(store.getGstin());
        }
        if (storeMeta.length() > 0) {
            lines.add(PdfLine.of(storeMeta.toString()));
        }
        lines.add(PdfLine.of(""));
        lines.add(PdfLine.bold("TAX INVOICE", 13));
        lines.add(
                PdfLine.of(
                        "Invoice No: "
                                + nz(invoice.getInvoiceNumber(), "-")
                                + "        Date: "
                                + (invoice.getInvoiceDate() != null
                                        ? invoice.getInvoiceDate().toString()
                                        : "-")));
        lines.add(PdfLine.of("Bill To   : " + fullName(customer)));
        if (isSet(customer.getPhone())) {
            lines.add(PdfLine.of("Phone     : " + customer.getPhone()));
        }
        lines.add(PdfLine.of(""));

        // Item table
        lines.add(
                PdfLine.bold(
                        rpad("Item", 40) + rpad("Qty", 6) + lpad("Rate", 16) + lpad("Amount", 16),
                        10));
        lines.add(PdfLine.of("-".repeat(WIDTH)));
        for (InvoiceItem item : items) {
            String name = itemNames.getOrDefault(item.getJewelryItemId(), "Item");
            lines.add(
                    PdfLine.of(
                            rpad(truncate(name, 40), 40)
                                    + rpad(String.valueOf(nz(item.getQuantity())), 6)
                                    + lpad(money(item.getMetalRate()), 16)
                                    + lpad(money(item.getTotalAmount()), 16)));
        }
        lines.add(PdfLine.of("-".repeat(WIDTH)));
        lines.add(PdfLine.of(""));

        // Totals (right-aligned)
        lines.add(totalLine("Subtotal", invoice.getSubtotal(), false));
        if (isPositive(invoice.getDiscount())) {
            lines.add(totalLine("Discount", invoice.getDiscount(), false));
        }
        if (isNonZero(invoice.getOldGoldAdjustment())) {
            lines.add(totalLine("Old Gold Adjustment", invoice.getOldGoldAdjustment(), false));
        }
        if (isPositive(invoice.getCgstAmount()) || isPositive(invoice.getSgstAmount())) {
            lines.add(totalLine("CGST", invoice.getCgstAmount(), false));
            lines.add(totalLine("SGST", invoice.getSgstAmount(), false));
        } else if (isPositive(invoice.getGstAmount())) {
            lines.add(totalLine("GST", invoice.getGstAmount(), false));
        }
        if (isNonZero(invoice.getRoundOff())) {
            lines.add(totalLine("Round Off", invoice.getRoundOff(), false));
        }
        lines.add(totalLine("TOTAL", invoice.getTotalAmount(), true));
        lines.add(PdfLine.of(""));
        lines.add(totalLine("Paid", invoice.getPaidAmount(), false));
        lines.add(
                totalLine(
                        "Balance",
                        nz(invoice.getTotalAmount()).subtract(nz(invoice.getPaidAmount())),
                        false));
        if (invoice.getPaymentStatus() != null) {
            lines.add(PdfLine.of(lpad("Status: " + invoice.getPaymentStatus().name(), WIDTH)));
        }
        lines.add(PdfLine.of(""));
        lines.add(PdfLine.of(""));
        lines.add(PdfLine.of("Thank you for shopping with " + nz(store.getName(), "us") + "!"));
        return lines;
    }

    private Map<Long, String> itemNames(List<InvoiceItem> items) {
        List<Long> ids =
                items.stream()
                        .map(InvoiceItem::getJewelryItemId)
                        .filter(id -> id != null)
                        .distinct()
                        .collect(Collectors.toList());
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            for (JewelryItem ji : jewelryItemRepository.findAllById(ids)) {
                names.put(ji.getId(), ji.getName());
            }
        }
        return names;
    }

    // ── formatting helpers ──────────────────────────────────────────

    private PdfLine totalLine(String label, BigDecimal value, boolean bold) {
        String s = label + ": " + money(value);
        return new PdfLine(lpad(s, WIDTH), bold, bold ? 11 : 10);
    }

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private static String money(BigDecimal v) {
        return "Rs. " + MONEY.format(nz(v));
    }

    private static String fullName(Customer c) {
        String last = c.getLastName();
        return isSet(last) ? c.getFirstName() + " " + last : nz(c.getFirstName(), "Customer");
    }

    private static String rpad(String s, int n) {
        if (s.length() >= n) {
            return s.substring(0, n);
        }
        return s + " ".repeat(n - s.length());
    }

    private static String lpad(String s, int n) {
        if (s.length() >= n) {
            return s;
        }
        return " ".repeat(n - s.length()) + s;
    }

    private static String truncate(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 3) + "...";
    }

    private static boolean isSet(String s) {
        return s != null && !s.isBlank();
    }

    private static String nz(String s, String fallback) {
        return isSet(s) ? s : fallback;
    }

    private static int nz(Integer i) {
        return i != null ? i : 0;
    }

    private static BigDecimal nz(BigDecimal b) {
        return b != null ? b : BigDecimal.ZERO;
    }

    private static boolean isPositive(BigDecimal b) {
        return b != null && b.signum() > 0;
    }

    private static boolean isNonZero(BigDecimal b) {
        return b != null && b.signum() != 0;
    }
}
