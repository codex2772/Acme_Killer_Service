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

import com.aurajewels.jewel.dto.messaging.BulkMessageRequest;
import com.aurajewels.jewel.dto.messaging.CampaignResponse;
import com.aurajewels.jewel.dto.messaging.MessageResponse;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.MessageCampaign;
import com.aurajewels.jewel.entity.MessageTemplate;
import com.aurajewels.jewel.entity.SchemeMember;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.MessageCampaignRepository;
import com.aurajewels.jewel.repository.SchemeMemberRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and runs bulk campaigns: resolves the audience, filters by opt-in, dispatches each
 * message through {@link MessagingService} (so all gating/logging/idempotency still applies) and
 * tracks counters.
 *
 * <p>Sends run synchronously in this milestone; messaging-tier throttling and scheduling are future
 * work. Delivered/read counters are updated asynchronously by the webhook as reports arrive.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageCampaignService {

    private final MessageCampaignRepository campaignRepository;
    private final TemplateResolver templateResolver;
    private final CustomerRepository customerRepository;
    private final SchemeMemberRepository schemeMemberRepository;
    private final StoreRepository storeRepository;
    private final MessagingService messagingService;
    private final OptInGuard optInGuard;

    public CampaignResponse createAndSend(Long storeId, BulkMessageRequest request, Long userId) {
        MessageTemplate template = templateResolver.resolve(storeId, request.getTemplateCode());
        MessageCampaign.AudienceType audienceType = parseAudience(request.getAudienceType());

        List<Customer> audience = resolveAudience(storeId, audienceType, request);
        List<Customer> recipients =
                audience.stream()
                        .filter(c -> optInGuard.allowed(c, template.getCategory()))
                        .toList();

        MessageCampaign campaign =
                MessageCampaign.builder()
                        .store(storeRepository.getReferenceById(storeId))
                        .name(request.getName() != null ? request.getName() : template.getName())
                        .template(template)
                        .audienceType(audienceType)
                        .totalRecipients(recipients.size())
                        .sentCount(0)
                        .deliveredCount(0)
                        .readCount(0)
                        .failedCount(0)
                        .status(MessageCampaign.CampaignStatus.SENDING)
                        .createdBy(userId)
                        .build();
        campaign.setActive(true);
        campaign = campaignRepository.save(campaign);

        int sent = 0;
        int failed = 0;
        for (Customer c : recipients) {
            MessageResponse r =
                    messagingService.sendTemplated(
                            storeId,
                            c.getId(),
                            request.getTemplateCode(),
                            request.getVariables(),
                            request.getMediaUrl(),
                            null,
                            null,
                            userId,
                            campaign.getId());
            if ("SENT".equals(r.getStatus())) {
                sent++;
            } else {
                failed++;
            }
        }

        campaign.setSentCount(sent);
        campaign.setFailedCount(failed);
        campaign.setStatus(MessageCampaign.CampaignStatus.COMPLETED);
        campaign.setCompletedAt(Instant.now());
        campaign = campaignRepository.save(campaign);

        log.info(
                "Campaign {} completed: {} recipients, {} sent, {} failed",
                campaign.getId(),
                recipients.size(),
                sent,
                failed);
        return toResponse(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listCampaigns(Long storeId, int limit) {
        return campaignRepository
                .findByStore_IdOrderByCreatedAtDesc(storeId, PageRequest.of(0, limit))
                .map(this::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(Long storeId, Long id) {
        MessageCampaign campaign =
                campaignRepository
                        .findById(id)
                        .filter(c -> c.getStore() != null && storeId.equals(c.getStore().getId()))
                        .orElseThrow(
                                () -> new IllegalArgumentException("Campaign not found: " + id));
        return toResponse(campaign);
    }

    // ── Audience resolution ─────────────────────────────────────────

    private List<Customer> resolveAudience(
            Long storeId, MessageCampaign.AudienceType type, BulkMessageRequest request) {
        return switch (type) {
            case ALL_CUSTOMERS -> customerRepository.findByStoreIdAndActiveTrue(storeId);
            case SCHEME_MEMBERS -> schemeMembers(storeId);
            case CUSTOM_LIST -> customList(storeId, request.getCustomerIds());
            case BIRTHDAY_TODAY, ANNIVERSARY_TODAY ->
                    throw new UnsupportedOperationException(
                            "Birthday/anniversary audiences require customer date-of-birth data,"
                                    + " which is not yet captured.");
        };
    }

    private List<Customer> schemeMembers(Long storeId) {
        List<SchemeMember> members =
                schemeMemberRepository.findByScheme_Store_IdAndStatus(
                        storeId, SchemeMember.MemberStatus.ACTIVE);
        Map<Long, Customer> distinct = new LinkedHashMap<>();
        for (SchemeMember m : members) {
            Customer c = m.getCustomer();
            if (c != null && Boolean.TRUE.equals(c.getActive())) {
                distinct.putIfAbsent(c.getId(), c);
            }
        }
        return new ArrayList<>(distinct.values());
    }

    private List<Customer> customList(Long storeId, List<Long> customerIds) {
        List<Customer> result = new ArrayList<>();
        if (customerIds == null) {
            return result;
        }
        for (Long id : customerIds) {
            customerRepository.findByIdAndStoreId(id, storeId).ifPresent(result::add);
        }
        return result;
    }

    private CampaignResponse toResponse(MessageCampaign c) {
        return CampaignResponse.builder()
                .campaignId(c.getId())
                .name(c.getName())
                .audienceType(c.getAudienceType() != null ? c.getAudienceType().name() : null)
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .totalRecipients(c.getTotalRecipients())
                .sentCount(c.getSentCount())
                .deliveredCount(c.getDeliveredCount())
                .readCount(c.getReadCount())
                .failedCount(c.getFailedCount())
                .completedAt(c.getCompletedAt())
                .build();
    }

    private MessageCampaign.AudienceType parseAudience(String value) {
        if (value == null) {
            throw new IllegalArgumentException("audienceType is required");
        }
        try {
            return MessageCampaign.AudienceType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown audienceType: " + value);
        }
    }
}
