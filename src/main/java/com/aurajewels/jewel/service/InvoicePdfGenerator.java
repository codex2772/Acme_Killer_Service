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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Minimal, dependency-free PDF writer. Lays out lines of monospaced (Courier) text onto A4 pages
 * and emits a valid PDF byte stream — no external PDF library required (the build environment
 * cannot resolve one). Sufficient for a text invoice/bill; swap for a richer generator (OpenPDF /
 * HTML→PDF) if fancier layout is ever needed.
 *
 * @author Raviraj Bhosale
 */
@Component
public class InvoicePdfGenerator {

    /** A single text line. */
    public record PdfLine(String text, boolean bold, int size) {
        public static PdfLine of(String text) {
            return new PdfLine(text, false, 10);
        }

        public static PdfLine bold(String text, int size) {
            return new PdfLine(text, true, size);
        }
    }

    private static final int PAGE_WIDTH = 595; // A4 points
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN_LEFT = 50;
    private static final int MARGIN_BOTTOM = 50;
    private static final int TOP_Y = 800;

    public byte[] generate(List<PdfLine> lines) {
        // 1. Paginate + build one content stream per page.
        List<String> pageStreams = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        int y = TOP_Y;
        for (PdfLine line : lines) {
            int lineHeight = line.size() + 4;
            if (y - lineHeight < MARGIN_BOTTOM) {
                pageStreams.add(content.toString());
                content = new StringBuilder();
                y = TOP_Y;
            }
            String font = line.bold() ? "F2" : "F1";
            content.append("BT\n")
                    .append('/')
                    .append(font)
                    .append(' ')
                    .append(line.size())
                    .append(" Tf\n")
                    .append("1 0 0 1 ")
                    .append(MARGIN_LEFT)
                    .append(' ')
                    .append(y)
                    .append(" Tm\n")
                    .append('(')
                    .append(escape(line.text()))
                    .append(") Tj\n")
                    .append("ET\n");
            y -= lineHeight;
        }
        pageStreams.add(content.toString());

        // 2. Assemble PDF objects, tracking byte offsets for the xref table.
        // Object numbering: 1 Catalog, 2 Pages, 3 Font Courier, 4 Font Courier-Bold,
        // then per page p: page = 5+2p, content = 6+2p.
        int pageCount = pageStreams.size();
        int objectCount = 4 + pageCount * 2;
        List<Integer> offsets = new ArrayList<>(); // offsets.get(n-1) = byte offset of object n
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        write(out, "%PDF-1.4\n");

        // obj 1: Catalog
        offsets.add(out.size());
        write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // obj 2: Pages
        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < pageCount; p++) {
            kids.append(5 + 2 * p).append(" 0 R ");
        }
        offsets.add(out.size());
        write(
                out,
                "2 0 obj\n<< /Type /Pages /Kids [ "
                        + kids
                        + "] /Count "
                        + pageCount
                        + " /MediaBox [0 0 "
                        + PAGE_WIDTH
                        + " "
                        + PAGE_HEIGHT
                        + "] >>\nendobj\n");

        // obj 3 + 4: fonts
        offsets.add(out.size());
        write(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n");
        offsets.add(out.size());
        write(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold >>\nendobj\n");

        // page + content objects
        for (int p = 0; p < pageCount; p++) {
            int pageNum = 5 + 2 * p;
            int contentNum = 6 + 2 * p;

            offsets.add(out.size());
            write(
                    out,
                    pageNum
                            + " 0 obj\n<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 3 0 R"
                            + " /F2 4 0 R >> >> /Contents "
                            + contentNum
                            + " 0 R >>\nendobj\n");

            byte[] streamBytes = pageStreams.get(p).getBytes(StandardCharsets.ISO_8859_1);
            offsets.add(out.size());
            write(out, contentNum + " 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
            out.writeBytes(streamBytes);
            write(out, "\nendstream\nendobj\n");
        }

        // 3. xref table + trailer
        int xrefOffset = out.size();
        write(out, "xref\n0 " + (objectCount + 1) + "\n");
        write(out, "0000000000 65535 f \n");
        for (int n = 1; n <= objectCount; n++) {
            write(out, String.format("%010d %05d n \n", offsets.get(n - 1), 0));
        }
        write(
                out,
                "trailer\n<< /Size "
                        + (objectCount + 1)
                        + " /Root 1 0 R >>\nstartxref\n"
                        + xrefOffset
                        + "\n%%EOF\n");

        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    /** Escape characters that are special in PDF literal strings, drop non-Latin-1. */
    private static String escape(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\').append(c);
            } else if (c >= 32 && c <= 255) {
                sb.append(c);
            } else {
                sb.append('?'); // Courier/WinAnsi can't render it (e.g. the rupee glyph)
            }
        }
        return sb.toString();
    }
}
