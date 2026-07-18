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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aurajewels.jewel.config.WhatsAppProperties;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.MessageLog;
import com.aurajewels.jewel.entity.MessageTemplate;
import com.aurajewels.jewel.entity.StoreWaba;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.MessageLogRepository;
import com.aurajewels.jewel.repository.StoreWabaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MessagingServiceTest {

    private WhatsAppProperties properties;
    private StoreWabaRepository storeWabaRepository;
    private CustomerRepository customerRepository;
    private MessageLogRepository messageLogRepository;
    private TemplateResolver templateResolver;
    private WhatsAppProvider whatsAppProvider;
    private MessagingService service;

    @BeforeEach
    void setUp() {
        properties = new WhatsAppProperties();
        properties.setEnabled(true);
        storeWabaRepository = Mockito.mock(StoreWabaRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        messageLogRepository = Mockito.mock(MessageLogRepository.class);
        templateResolver = Mockito.mock(TemplateResolver.class);
        whatsAppProvider = Mockito.mock(WhatsAppProvider.class);

        // save() echoes the entity back, assigning an id when absent.
        when(messageLogRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            MessageLog m = inv.getArgument(0);
                            if (m.getId() == null) {
                                m.setId(99L);
                            }
                            return m;
                        });

        service =
                new MessagingService(
                        properties,
                        storeWabaRepository,
                        customerRepository,
                        messageLogRepository,
                        templateResolver,
                        whatsAppProvider,
                        new OptInGuard());
    }

    private StoreWaba connectedWaba() {
        StoreWaba waba = new StoreWaba();
        waba.setStatus(StoreWaba.WabaStatus.CONNECTED);
        waba.setPhoneNumberId("PN1");
        waba.setAccessTokenRef("enc:token");
        return waba;
    }

    private Customer customer() {
        Customer c = new Customer();
        c.setFirstName("Raviraj");
        c.setPhone("9876543210");
        return c;
    }

    private MessageTemplate utilityTemplate() {
        MessageTemplate t = new MessageTemplate();
        t.setCategory(MessageTemplate.TemplateCategory.UTILITY);
        t.setMetaTemplateName("hello_world");
        t.setMetaLanguage("en_US");
        t.setHeaderType(MessageTemplate.HeaderType.NONE);
        return t;
    }

    @Test
    void killSwitchSkipsWithoutCallingProvider() {
        properties.setEnabled(false);

        var response =
                service.sendTemplated(1L, 10L, "HELLO_WORLD", Map.of(), null, null, null, null);

        assertThat(response.getStatus()).isEqualTo("SKIPPED_FEATURE_OFF");
        verify(whatsAppProvider, never()).sendTemplate(any(), any());
    }

    @Test
    void duplicateAutoTriggerIsSkipped() {
        when(messageLogRepository.existsByStoreIdAndCustomerIdAndRefTypeAndRefId(
                        1L, 10L, "INVOICE", 5L))
                .thenReturn(true);

        var response =
                service.sendTemplated(
                        1L, 10L, "INVOICE_CONFIRMATION", Map.of(), null, "INVOICE", 5L, null);

        assertThat(response.getStatus()).isEqualTo("SKIPPED_DUPLICATE");
        verify(whatsAppProvider, never()).sendTemplate(any(), any());
        verify(messageLogRepository, never()).save(any());
    }

    @Test
    void notConnectedStoreSkipsNoChannel() {
        when(storeWabaRepository.findByStore_Id(1L)).thenReturn(Optional.empty());

        var response =
                service.sendTemplated(1L, 10L, "HELLO_WORLD", Map.of(), null, null, null, null);

        assertThat(response.getStatus()).isEqualTo("SKIPPED_NO_CHANNEL");
        verify(whatsAppProvider, never()).sendTemplate(any(), any());
    }

    @Test
    void happyPathSendsAndRecordsSent() {
        when(storeWabaRepository.findByStore_Id(1L)).thenReturn(Optional.of(connectedWaba()));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer()));
        MessageTemplate template = utilityTemplate();
        when(templateResolver.resolve(1L, "HELLO_WORLD")).thenReturn(template);
        when(templateResolver.render(eq(template), any()))
                .thenReturn(new TemplateResolver.RenderedMessage("Hello World", List.of()));
        when(whatsAppProvider.sendTemplate(any(), any())).thenReturn(SendResult.ok("wamid.XYZ"));

        var response =
                service.sendTemplated(1L, 10L, "HELLO_WORLD", Map.of(), null, null, null, 7L);

        assertThat(response.getStatus()).isEqualTo("SENT");
        assertThat(response.getProviderMessageId()).isEqualTo("wamid.XYZ");
        verify(whatsAppProvider).sendTemplate(any(), any());
    }

    @Test
    void marketingWithoutOptInIsSkipped() {
        when(storeWabaRepository.findByStore_Id(1L)).thenReturn(Optional.of(connectedWaba()));
        when(customerRepository.findById(10L)).thenReturn(Optional.of(customer()));
        MessageTemplate marketing = utilityTemplate();
        marketing.setCategory(MessageTemplate.TemplateCategory.MARKETING);
        when(templateResolver.resolve(1L, "FESTIVAL")).thenReturn(marketing);

        var response = service.sendTemplated(1L, 10L, "FESTIVAL", Map.of(), null, null, null, null);

        assertThat(response.getStatus()).isEqualTo("SKIPPED_NOT_OPTED_IN");
        verify(whatsAppProvider, never()).sendTemplate(any(), any());
    }
}
