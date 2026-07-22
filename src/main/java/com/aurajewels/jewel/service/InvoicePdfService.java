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
import com.aurajewels.jewel.entity.InvoicePayment;
import com.aurajewels.jewel.entity.JewelryItem;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.InvoiceRepository;
import com.aurajewels.jewel.repository.JewelryItemRepository;
import com.aurajewels.jewel.service.InvoicePdfGenerator.PdfLine;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
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
    private static final String TEMPLATE_PATH = "templates/invoice-template.html";
    private static final String DEFAULT_TAGLINE = "Fine Jewellery";
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private final InvoiceRepository invoiceRepository;
    private final JewelryItemRepository jewelryItemRepository;
    private final InvoicePdfGenerator pdfGenerator;
    private final S3Service s3Service;
    private final HtmlToPdfRenderer htmlRenderer;

    private volatile String templateCache;

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
        byte[] pdf = renderPdf(invoice);
        String url = s3Service.uploadBytes(pdf, "application/pdf", "invoices", ".pdf");
        log.info("Generated invoice PDF for {} -> {}", invoice.getInvoiceNumber(), url);
        return url;
    }

    /** Branded HTML→Chromium PDF, falling back to the plain-text PDF if rendering fails. */
    private byte[] renderPdf(Invoice invoice) {
        try {
            String html = MustacheLite.render(loadTemplate(), buildDataModel(invoice));
            return htmlRenderer.render(html);
        } catch (Exception e) {
            log.warn(
                    "Branded HTML→PDF failed for invoice {} ({}); using plain-text fallback",
                    invoice.getInvoiceNumber(),
                    e.getMessage());
            return pdfGenerator.generate(buildLines(invoice));
        }
    }

    private String loadTemplate() {
        String t = templateCache;
        if (t == null) {
            try (var in = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
                t = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                templateCache = t;
            } catch (IOException e) {
                throw new IllegalStateException("Invoice template not found: " + TEMPLATE_PATH, e);
            }
        }
        return t;
    }

    // ── branded HTML data model (matches invoice-template-spec.html placeholders) ──

    private Map<String, Object> buildDataModel(Invoice invoice) {
        Store store = invoice.getStore();
        Customer customer = invoice.getCustomer();
        List<InvoiceItem> items = invoice.getItems() != null ? invoice.getItems() : List.of();
        Map<Long, JewelryItem> jewelById = itemsById(items);

        Map<String, Object> m = new HashMap<>();

        // Store
        m.put("businessName", nz(store.getName(), "Jewellery Store"));
        m.put("tagline", DEFAULT_TAGLINE);
        m.put("storePhone", nz(store.getPhone(), ""));
        m.put("storeEmail", ""); // not modelled on Store
        m.put("storeWebsite", "");
        m.put("storeGstin", nz(store.getGstin(), ""));

        // Invoice
        boolean cancelled = invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED;
        m.put("typeLabel", typeLabel(invoice.getInvoiceType()));
        m.put("invoiceId", nz(invoice.getInvoiceNumber(), "-"));
        m.put("date", fmtDate(invoice.getInvoiceDate()));
        if (invoice.getDueDate() != null) {
            m.put("dueDate", fmtDate(invoice.getDueDate()));
        }
        m.put("storeName", nz(store.getName(), "-"));
        m.put("status", invoice.getStatus() != null ? titleCase(invoice.getStatus().name()) : "-");
        m.put(
                "paymentMode",
                invoice.getPaymentMode() != null
                        ? titleCase(invoice.getPaymentMode().name())
                        : "-");
        m.put("isCancelled", cancelled);
        m.put("itemCount", items.size());
        putIfSet(m, "notes", invoice.getNotes());

        // Customer
        m.put("customerName", customer != null ? fullName(customer) : "Walk-in Customer");
        if (customer != null) {
            putIfSet(m, "customerPhone", customer.getPhone());
            putIfSet(m, "customerEmail", customer.getEmail());
            putIfSet(m, "customerAddress", customer.getAddressLine1());
            putIfSet(m, "customerCity", customer.getCity());
            putIfSet(m, "customerGstin", customer.getGstin());
            putIfSet(m, "customerPan", customer.getPan());
        }

        // Items
        List<Map<String, Object>> itemRows = new ArrayList<>();
        int idx = 1;
        for (InvoiceItem item : items) {
            JewelryItem ji = jewelById.get(item.getJewelryItemId());
            Map<String, Object> row = new HashMap<>();
            row.put("index", idx++);
            row.put("name", ji != null ? nz(ji.getName(), "Item") : "Item");
            row.put(
                    "purity",
                    ji != null && ji.getMetalType() != null && isSet(ji.getMetalType().getPurity())
                            ? ji.getMetalType().getPurity()
                            : "—");
            row.put("hsn", ji != null && isSet(ji.getHsnCode()) ? ji.getHsnCode() : "7113");
            row.put("weight", IndianMoney.weight(ji != null ? ji.getNetWeight() : null));
            row.put("rate", IndianMoney.rupees(item.getMetalRate()));
            row.put("amount", IndianMoney.rupees(item.getTotalAmount()));
            itemRows.add(row);
        }
        m.put("items", itemRows);

        // Payments
        List<Map<String, Object>> payRows = new ArrayList<>();
        if (invoice.getPayments() != null) {
            for (InvoicePayment p : invoice.getPayments()) {
                Map<String, Object> row = new HashMap<>();
                row.put("mode", nz(p.getMode(), "-"));
                putIfSet(row, "reference", p.getReference());
                row.put("amount", IndianMoney.rupees(p.getAmount()));
                payRows.add(row);
            }
        }
        m.put("payments", payRows);

        // Summary — CGST/SGST rate is half the invoice GST rate
        BigDecimal halfRate =
                nz(invoice.getGstRate()).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        m.put("subtotal", IndianMoney.rupees(invoice.getSubtotal()));
        m.put("cgstRate", plainRate(halfRate));
        m.put("cgstAmount", IndianMoney.rupees(invoice.getCgstAmount()));
        m.put("sgstRate", plainRate(halfRate));
        m.put("sgstAmount", IndianMoney.rupees(invoice.getSgstAmount()));
        if (isPositive(invoice.getDiscount())) {
            m.put("discount", IndianMoney.rupees(invoice.getDiscount()));
        }
        if (isNonZero(invoice.getOldGoldAdjustment())) {
            m.put("oldGoldAdj", IndianMoney.rupees(invoice.getOldGoldAdjustment()));
        }
        m.put("oldGoldItems", List.of()); // no per-item old-gold breakdown on the invoice model
        if (isNonZero(invoice.getRoundOff())) {
            m.put("roundOff", IndianMoney.rupees(invoice.getRoundOff()));
        }
        m.put("grandTotal", IndianMoney.rupees(invoice.getTotalAmount()));
        m.put("amountPaid", IndianMoney.rupees(invoice.getPaidAmount()));
        BigDecimal balance = nz(invoice.getTotalAmount()).subtract(nz(invoice.getPaidAmount()));
        boolean paid = balance.signum() <= 0;
        m.put("balanceDueIsZero", paid);
        m.put("balanceDueLabel", paid ? "₹0 — Paid" : IndianMoney.rupees(balance));
        m.put("amountInWords", IndianMoney.rupeesInWords(invoice.getTotalAmount()));
        return m;
    }

    private Map<Long, JewelryItem> itemsById(List<InvoiceItem> items) {
        List<Long> ids =
                items.stream()
                        .map(InvoiceItem::getJewelryItemId)
                        .filter(id -> id != null)
                        .distinct()
                        .collect(Collectors.toList());
        Map<Long, JewelryItem> byId = new HashMap<>();
        if (!ids.isEmpty()) {
            for (JewelryItem ji : jewelryItemRepository.findAllById(ids)) {
                byId.put(ji.getId(), ji);
            }
        }
        return byId;
    }

    private static String typeLabel(Invoice.InvoiceType type) {
        if (type == null) {
            return "Tax Invoice";
        }
        return switch (type.name()) {
            case "CREDIT_NOTE" -> "Credit Note";
            case "ESTIMATE" -> "Estimate";
            default -> "Tax Invoice";
        };
    }

    private static String fmtDate(LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "-";
    }

    private static String titleCase(String s) {
        if (!isSet(s)) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (String word : s.toLowerCase(Locale.ENGLISH).split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private static String plainRate(BigDecimal v) {
        BigDecimal r = nz(v).stripTrailingZeros();
        if (r.scale() < 0) {
            r = r.setScale(0, RoundingMode.UNNECESSARY);
        }
        return r.toPlainString();
    }

    private static void putIfSet(Map<String, Object> m, String key, String value) {
        if (isSet(value)) {
            m.put(key, value);
        }
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
