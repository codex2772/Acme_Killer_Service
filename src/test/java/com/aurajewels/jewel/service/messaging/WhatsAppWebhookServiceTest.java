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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aurajewels.jewel.config.WhatsAppProperties;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.CustomerEnquiry;
import com.aurajewels.jewel.entity.MessageLog;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.entity.StoreWaba;
import com.aurajewels.jewel.repository.CustomerEnquiryRepository;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.MessageCampaignRepository;
import com.aurajewels.jewel.repository.MessageLogRepository;
import com.aurajewels.jewel.repository.StoreWabaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class WhatsAppWebhookServiceTest {

    private MessageLogRepository messageLogRepository;
    private StoreWabaRepository storeWabaRepository;
    private CustomerRepository customerRepository;
    private CustomerEnquiryRepository customerEnquiryRepository;
    private MessageCampaignRepository campaignRepository;
    private WhatsAppWebhookService service;

    @BeforeEach
    void setUp() {
        messageLogRepository = Mockito.mock(MessageLogRepository.class);
        storeWabaRepository = Mockito.mock(StoreWabaRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        customerEnquiryRepository = Mockito.mock(CustomerEnquiryRepository.class);
        campaignRepository = Mockito.mock(MessageCampaignRepository.class);

        WhatsAppProperties props = new WhatsAppProperties();
        props.getMeta().setWebhookVerifyToken("verify-me");
        props.getMeta().setAppSecret("placeholder"); // skips signature check in tests

        service =
                new WhatsAppWebhookService(
                        props,
                        new ObjectMapper(),
                        messageLogRepository,
                        storeWabaRepository,
                        customerRepository,
                        customerEnquiryRepository,
                        campaignRepository);
    }

    @Test
    void verifySubscriptionEchoesChallengeOnTokenMatch() {
        assertThat(service.verifySubscription("subscribe", "verify-me", "CH123")).contains("CH123");
        assertThat(service.verifySubscription("subscribe", "wrong", "CH123")).isEmpty();
    }

    @Test
    void deliveredStatusAdvancesLogForward() {
        MessageLog logRow = new MessageLog();
        logRow.setStatus(MessageLog.MessageStatus.SENT);
        when(messageLogRepository.findByProviderMessageId("wamid.ABC"))
                .thenReturn(Optional.of(logRow));

        service.handleEvent(
                """
                {"entry":[{"changes":[{"value":{
                  "metadata":{"phone_number_id":"PN1"},
                  "statuses":[{"id":"wamid.ABC","status":"delivered","timestamp":"1700000000"}]
                }}]}]}
                """,
                null);

        assertThat(logRow.getStatus()).isEqualTo(MessageLog.MessageStatus.DELIVERED);
        assertThat(logRow.getDeliveredAt()).isNotNull();
        verify(messageLogRepository).save(logRow);
    }

    @Test
    void readStatusIsNotDowngradedByLateDelivered() {
        MessageLog logRow = new MessageLog();
        logRow.setStatus(MessageLog.MessageStatus.READ);
        when(messageLogRepository.findByProviderMessageId("wamid.ABC"))
                .thenReturn(Optional.of(logRow));

        service.handleEvent(
                """
                {"entry":[{"changes":[{"value":{
                  "metadata":{"phone_number_id":"PN1"},
                  "statuses":[{"id":"wamid.ABC","status":"delivered","timestamp":"1700000000"}]
                }}]}]}
                """,
                null);

        assertThat(logRow.getStatus()).isEqualTo(MessageLog.MessageStatus.READ);
    }

    @Test
    void inboundFromKnownCustomerCreatesEnquiry() {
        Store store = new Store();
        store.setId(1L);
        StoreWaba waba = new StoreWaba();
        waba.setStore(store);
        Customer customer = new Customer();
        customer.setStore(store);
        customer.setFirstName("Raviraj");

        when(storeWabaRepository.findByPhoneNumberId("PN1")).thenReturn(Optional.of(waba));
        when(customerRepository.findByPhoneAndStoreId("919876543210", 1L))
                .thenReturn(Optional.empty());
        when(customerRepository.findByPhoneAndStoreId("9876543210", 1L))
                .thenReturn(Optional.of(customer));

        service.handleEvent(
                """
                {"entry":[{"changes":[{"value":{
                  "metadata":{"phone_number_id":"PN1"},
                  "messages":[{"from":"919876543210","type":"text",
                               "text":{"body":"Is my order ready?"}}]
                }}]}]}
                """,
                null);

        assertThat(customer.getHasWhatsapp()).isTrue();
        ArgumentCaptor<CustomerEnquiry> captor = ArgumentCaptor.forClass(CustomerEnquiry.class);
        verify(customerEnquiryRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("Is my order ready?");
        assertThat(captor.getValue().getStatus()).isEqualTo(CustomerEnquiry.EnquiryStatus.OPEN);
    }

    @Test
    void inboundFromUnknownNumberCreatesNoEnquiry() {
        Store store = new Store();
        store.setId(1L);
        StoreWaba waba = new StoreWaba();
        waba.setStore(store);
        when(storeWabaRepository.findByPhoneNumberId("PN1")).thenReturn(Optional.of(waba));
        when(customerRepository.findByPhoneAndStoreId(any(), any())).thenReturn(Optional.empty());

        service.handleEvent(
                """
                {"entry":[{"changes":[{"value":{
                  "metadata":{"phone_number_id":"PN1"},
                  "messages":[{"from":"910000000000","type":"text","text":{"body":"hi"}}]
                }}]}]}
                """,
                null);

        verify(customerEnquiryRepository, never()).save(any());
    }
}
