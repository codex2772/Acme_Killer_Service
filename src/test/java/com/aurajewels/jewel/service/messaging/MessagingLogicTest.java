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

import static org.assertj.core.api.Assertions.assertThat;

import com.aurajewels.jewel.config.WhatsAppProperties;
import com.aurajewels.jewel.entity.MessageTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit tests for the pure messaging logic: phone normalisation, rendering, token round-trip. */
class MessagingLogicTest {

    @Test
    void normalisesIndianMobileNumbersToE164() {
        assertThat(MessagingService.toE164("9876543210")).isEqualTo("919876543210");
        assertThat(MessagingService.toE164("09876543210")).isEqualTo("919876543210");
        assertThat(MessagingService.toE164("+91 98765 43210")).isEqualTo("919876543210");
        assertThat(MessagingService.toE164("919876543210")).isEqualTo("919876543210");
        assertThat(MessagingService.toE164(null)).isEmpty();
    }

    @Test
    void rendersOrderedTemplateVariables() {
        TemplateResolver resolver = new TemplateResolver(null, new ObjectMapper());
        MessageTemplate template = new MessageTemplate();
        template.setBodyTemplate("Dear {{1}}, invoice {{2}} of Rs.{{3}} from {{4}} is confirmed.");
        template.setVariables("[\"customerName\",\"invoiceNo\",\"amount\",\"storeName\"]");

        TemplateResolver.RenderedMessage rendered =
                resolver.render(
                        template,
                        Map.of(
                                "customerName", "Raviraj",
                                "invoiceNo", "INV-15",
                                "amount", "1,11,240",
                                "storeName", "Prakash Jewellers"));

        assertThat(rendered.params())
                .containsExactly("Raviraj", "INV-15", "1,11,240", "Prakash Jewellers");
        assertThat(rendered.text())
                .isEqualTo(
                        "Dear Raviraj, invoice INV-15 of Rs.1,11,240 from Prakash Jewellers is"
                                + " confirmed.");
    }

    @Test
    void detectsLanguageMismatchErrorsForEnUsFallback() {
        // Meta 132001 / translation-not-found → should trigger the en_US retry
        assertThat(
                        MetaCloudWhatsAppProvider.isLanguageMismatch(
                                "(#132001) Template name does not exist in the translation en"))
                .isTrue();
        assertThat(
                        MetaCloudWhatsAppProvider.isLanguageMismatch(
                                "Template does not exist in the locale en"))
                .isTrue();
        // Unrelated errors must NOT trigger a retry
        assertThat(MetaCloudWhatsAppProvider.isLanguageMismatch("Recipient not in allowed list"))
                .isFalse();
        assertThat(MetaCloudWhatsAppProvider.isLanguageMismatch("Session has expired")).isFalse();
        assertThat(MetaCloudWhatsAppProvider.isLanguageMismatch(null)).isFalse();
    }

    @Test
    void encryptsAndResolvesTokenWithoutStoringPlaintext() {
        WhatsAppProperties props = new WhatsAppProperties();
        props.setTokenEncryptionKey("unit-test-key");
        LocalEncryptedWabaTokenStore store = new LocalEncryptedWabaTokenStore(props);

        String token = "EAAG_super_secret_meta_access_token_value";
        String ref = store.store(1L, token);

        assertThat(ref).startsWith("enc:").doesNotContain(token);
        assertThat(store.resolve(ref)).isEqualTo(token);
    }
}
