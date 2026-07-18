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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aurajewels.jewel.dto.messaging.BulkMessageRequest;
import com.aurajewels.jewel.dto.messaging.CampaignResponse;
import com.aurajewels.jewel.dto.messaging.MessageResponse;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.MessageCampaign;
import com.aurajewels.jewel.entity.MessageTemplate;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.MessageCampaignRepository;
import com.aurajewels.jewel.repository.SchemeMemberRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MessageCampaignServiceTest {

    private MessageCampaignRepository campaignRepository;
    private TemplateResolver templateResolver;
    private CustomerRepository customerRepository;
    private SchemeMemberRepository schemeMemberRepository;
    private StoreRepository storeRepository;
    private MessagingService messagingService;
    private MessageCampaignService service;

    @BeforeEach
    void setUp() {
        campaignRepository = Mockito.mock(MessageCampaignRepository.class);
        templateResolver = Mockito.mock(TemplateResolver.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        schemeMemberRepository = Mockito.mock(SchemeMemberRepository.class);
        storeRepository = Mockito.mock(StoreRepository.class);
        messagingService = Mockito.mock(MessagingService.class);

        when(campaignRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            MessageCampaign c = inv.getArgument(0);
                            if (c.getId() == null) {
                                c.setId(500L);
                            }
                            return c;
                        });
        when(storeRepository.getReferenceById(anyLong())).thenReturn(new Store());

        service =
                new MessageCampaignService(
                        campaignRepository,
                        templateResolver,
                        customerRepository,
                        schemeMemberRepository,
                        storeRepository,
                        messagingService,
                        new OptInGuard());
    }

    private Customer customer(long id, boolean optIn) {
        Customer c = new Customer();
        c.setId(id);
        c.setFirstName("C" + id);
        c.setWhatsappOptIn(optIn);
        return c;
    }

    private MessageTemplate marketingTemplate() {
        MessageTemplate t = new MessageTemplate();
        t.setName("Festival");
        t.setCategory(MessageTemplate.TemplateCategory.MARKETING);
        return t;
    }

    @Test
    void marketingCampaignOnlySendsToOptedInCustomers() {
        when(templateResolver.resolve(1L, "FESTIVAL_OFFER")).thenReturn(marketingTemplate());
        when(customerRepository.findByStoreIdAndActiveTrue(1L))
                .thenReturn(List.of(customer(1, true), customer(2, false), customer(3, true)));
        when(messagingService.sendTemplated(
                        anyLong(), anyLong(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(MessageResponse.builder().status("SENT").build());

        BulkMessageRequest req = new BulkMessageRequest();
        req.setTemplateCode("FESTIVAL_OFFER");
        req.setAudienceType("ALL_CUSTOMERS");

        CampaignResponse response = service.createAndSend(1L, req, 7L);

        // 2 opted-in of 3 → 2 recipients, 2 sends
        assertThat(response.getTotalRecipients()).isEqualTo(2);
        assertThat(response.getSentCount()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(messagingService, times(2))
                .sendTemplated(
                        eq(1L),
                        anyLong(),
                        eq("FESTIVAL_OFFER"),
                        any(),
                        any(),
                        any(),
                        any(),
                        eq(7L),
                        eq(500L));
    }

    @Test
    void birthdayAudienceIsUnsupported() {
        when(templateResolver.resolve(1L, "FESTIVAL_OFFER")).thenReturn(marketingTemplate());
        BulkMessageRequest req = new BulkMessageRequest();
        req.setTemplateCode("FESTIVAL_OFFER");
        req.setAudienceType("BIRTHDAY_TODAY");

        assertThatThrownBy(() -> service.createAndSend(1L, req, 7L))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
