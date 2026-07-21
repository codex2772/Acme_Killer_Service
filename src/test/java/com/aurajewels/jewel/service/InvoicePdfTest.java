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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.Invoice;
import com.aurajewels.jewel.entity.InvoiceItem;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.InvoiceRepository;
import com.aurajewels.jewel.repository.JewelryItemRepository;
import com.aurajewels.jewel.service.InvoicePdfGenerator.PdfLine;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class InvoicePdfTest {

    private final InvoicePdfGenerator generator = new InvoicePdfGenerator();

    @Test
    void producesStructurallyValidPdf() {
        byte[] pdf =
                generator.generate(
                        List.of(
                                PdfLine.bold("PNG Jewellers", 16),
                                PdfLine.of("Invoice INV-00015 (amount Rs. 1,11,240.00)"),
                                PdfLine.of("Item with (parens) and a back\\slash")));
        String text = new String(pdf, StandardCharsets.ISO_8859_1);

        assertThat(text).startsWith("%PDF-1.4");
        assertThat(text).contains("/BaseFont /Courier");
        assertThat(text).endsWith("%%EOF\n");
        // special chars must be escaped in the content stream
        assertThat(text).contains("\\(parens\\)").contains("back\\\\slash");

        // startxref must point at the xref keyword
        int sx = text.lastIndexOf("startxref");
        int xrefOffset =
                Integer.parseInt(text.substring(sx + 10, text.indexOf('\n', sx + 10)).trim());
        assertThat(text.substring(xrefOffset)).startsWith("xref");

        // the xref offset recorded for object 1 must actually point at "1 0 obj"
        int entriesStart = text.indexOf('\n', text.indexOf('\n', xrefOffset) + 1) + 1;
        int obj1Offset = Integer.parseInt(text.substring(entriesStart + 20, entriesStart + 30));
        assertThat(text.substring(obj1Offset)).startsWith("1 0 obj");
    }

    @Test
    void paginatesLongContent() {
        // ~200 lines forces multiple pages; the PDF must still be valid with Count > 1
        List<PdfLine> many =
                java.util.stream.IntStream.range(0, 200)
                        .mapToObj(i -> PdfLine.of("line " + i))
                        .toList();
        String text = new String(generator.generate(many), StandardCharsets.ISO_8859_1);
        assertThat(text).contains("/Type /Pages").doesNotContain("/Count 1 ");
        assertThat(text).endsWith("%%EOF\n");
    }

    @Test
    void generatesAndUploadsInvoicePdf() {
        InvoiceRepository invoiceRepository = Mockito.mock(InvoiceRepository.class);
        JewelryItemRepository jewelryItemRepository = Mockito.mock(JewelryItemRepository.class);
        S3Service s3Service = Mockito.mock(S3Service.class);
        InvoicePdfService service =
                new InvoicePdfService(
                        invoiceRepository, jewelryItemRepository, generator, s3Service);

        Store store = Store.builder().name("PNG Jewellers").gstin("27ABCDE1234F1Z5").build();
        Customer customer = Customer.builder().firstName("Raviraj").phone("9876543210").build();
        InvoiceItem item =
                InvoiceItem.builder()
                        .jewelryItemId(100L)
                        .quantity(1)
                        .metalRate(new BigDecimal("6200.00"))
                        .totalAmount(new BigDecimal("111240.00"))
                        .build();
        Invoice invoice =
                Invoice.builder()
                        .store(store)
                        .customer(customer)
                        .items(List.of(item))
                        .invoiceNumber("INV-00015")
                        .totalAmount(new BigDecimal("111240.00"))
                        .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(jewelryItemRepository.findAllById(any())).thenReturn(List.of());
        String url = "https://bucket.s3.ap-south-1.amazonaws.com/invoices/uuid.pdf";
        when(s3Service.uploadBytes(any(), eq("application/pdf"), eq("invoices"), eq(".pdf")))
                .thenReturn(url);

        String result = service.generateAndUploadPdf(1L);

        assertThat(result).isEqualTo(url);
        ArgumentCaptor<byte[]> pdfCaptor = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(s3Service)
                .uploadBytes(
                        pdfCaptor.capture(), eq("application/pdf"), eq("invoices"), eq(".pdf"));
        assertThat(new String(pdfCaptor.getValue(), StandardCharsets.ISO_8859_1))
                .startsWith("%PDF");
    }
}
