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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Renders an HTML string to a PDF by shelling out to headless Chromium (the same engine the desktop
 * app prints with, so the branded invoice matches). Throws on any failure so the caller can fall
 * back to the plain-text PDF.
 *
 * <p>Backgrounds/gradients print because the template sets {@code print-color-adjust: exact}; page
 * size/margins come from its {@code @page A4 8mm}. Fonts must be installed in the image (see the
 * Dockerfile) or the serif silently falls back.
 *
 * @author Raviraj Bhosale
 */
@Component
@Slf4j
public class HtmlToPdfRenderer {

    @Value("${pdf.chromium-binary:chromium-browser}")
    private String chromiumBinary;

    @Value("${pdf.render-timeout-seconds:30}")
    private int timeoutSeconds;

    /**
     * @param html a complete HTML document
     * @return the rendered PDF bytes
     * @throws IOException if Chromium is missing, times out, or produces no output
     */
    public byte[] render(String html) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("invoice-pdf-");
        Path htmlFile = workDir.resolve("invoice.html");
        Path pdfFile = workDir.resolve("invoice.pdf");
        try {
            Files.writeString(htmlFile, html, StandardCharsets.UTF_8);

            List<String> cmd =
                    List.of(
                            chromiumBinary,
                            "--headless",
                            "--no-sandbox",
                            "--disable-gpu",
                            "--disable-dev-shm-usage",
                            "--user-data-dir=" + workDir.resolve("profile").toAbsolutePath(),
                            "--hide-scrollbars",
                            "--no-pdf-header-footer",
                            "--run-all-compositor-stages-before-draw",
                            "--virtual-time-budget=10000",
                            "--print-to-pdf=" + pdfFile.toAbsolutePath(),
                            htmlFile.toAbsolutePath().toUri().toString());

            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String output =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException(
                        "Chromium PDF render timed out after " + timeoutSeconds + "s");
            }
            if (process.exitValue() != 0 || !Files.exists(pdfFile) || Files.size(pdfFile) == 0) {
                throw new IOException(
                        "Chromium PDF render failed (exit "
                                + process.exitValue()
                                + "): "
                                + output.strip());
            }
            return Files.readAllBytes(pdfFile);
        } finally {
            deleteQuietly(pdfFile);
            deleteQuietly(htmlFile);
            deleteQuietly(workDir);
        }
    }

    private void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            log.debug("Could not delete temp path {}: {}", p, e.getMessage());
        }
    }
}
