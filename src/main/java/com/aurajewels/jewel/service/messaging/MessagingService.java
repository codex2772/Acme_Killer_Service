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

import com.aurajewels.jewel.config.WhatsAppProperties;
import com.aurajewels.jewel.dto.messaging.MessageResponse;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.MessageChannel;
import com.aurajewels.jewel.entity.MessageLog;
import com.aurajewels.jewel.entity.MessageTemplate;
import com.aurajewels.jewel.entity.StoreWaba;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.MessageLogRepository;
import com.aurajewels.jewel.repository.StoreWabaRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates outbound customer messaging: feature gating, channel selection, opt-in enforcement,
 * idempotency, template resolution, audit logging and provider dispatch. This is the single entry
 * point used by controllers today and by auto-trigger event listeners later.
 *
 * <p>In M2 the send is performed synchronously so the API response reflects the true outcome;
 * off-request async dispatch is introduced with the invoice auto-trigger in M4.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingService {

    private final WhatsAppProperties properties;
    private final StoreWabaRepository storeWabaRepository;
    private final CustomerRepository customerRepository;
    private final MessageLogRepository messageLogRepository;
    private final TemplateResolver templateResolver;
    private final WhatsAppProvider whatsAppProvider;
    private final OptInGuard optInGuard;

    /** Send a templated WhatsApp message to a customer (not part of a campaign). */
    public MessageResponse sendTemplated(
            Long storeId,
            Long customerId,
            String templateCode,
            Map<String, Object> variables,
            String mediaUrl,
            String refType,
            Long refId,
            Long sentBy) {
        return sendTemplated(
                storeId,
                customerId,
                templateCode,
                variables,
                mediaUrl,
                refType,
                refId,
                sentBy,
                null);
    }

    /**
     * Send a templated WhatsApp message to a customer.
     *
     * @param storeId sending store
     * @param customerId recipient customer
     * @param templateCode template to use
     * @param variables values for the template's named variables
     * @param mediaUrl optional media header link
     * @param refType/refId source entity for auto-triggers (enables idempotency); null for manual
     * @param sentBy acting user id, if any
     * @param campaignId owning campaign, if this send is part of one
     */
    public MessageResponse sendTemplated(
            Long storeId,
            Long customerId,
            String templateCode,
            Map<String, Object> variables,
            String mediaUrl,
            String refType,
            Long refId,
            Long sentBy,
            Long campaignId) {

        // 1. Global kill-switch
        if (!properties.isEnabled()) {
            return skip(
                    storeId,
                    customerId,
                    null,
                    null,
                    MessageLog.MessageStatus.SKIPPED_FEATURE_OFF,
                    "WhatsApp messaging disabled",
                    sentBy,
                    refType,
                    refId,
                    campaignId);
        }

        // 2. Idempotency for auto-triggered sends
        if (refType != null
                && refId != null
                && messageLogRepository.existsByStoreIdAndCustomerIdAndRefTypeAndRefId(
                        storeId, customerId, refType, refId)) {
            log.info(
                    "Skipping duplicate send store={} customer={} ref={}:{}",
                    storeId,
                    customerId,
                    refType,
                    refId);
            return MessageResponse.builder().status("SKIPPED_DUPLICATE").build();
        }

        // 3. Channel selection — store must have a connected WABA
        StoreWaba waba = storeWabaRepository.findByStore_Id(storeId).orElse(null);
        if (waba == null
                || waba.getStatus() != StoreWaba.WabaStatus.CONNECTED
                || waba.getPhoneNumberId() == null) {
            return skip(
                    storeId,
                    customerId,
                    null,
                    null,
                    MessageLog.MessageStatus.SKIPPED_NO_CHANNEL,
                    "Store WhatsApp not connected",
                    sentBy,
                    refType,
                    refId,
                    campaignId);
        }

        // 4. Load recipient
        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Customer not found: " + customerId));

        // 5. Resolve template
        MessageTemplate template = templateResolver.resolve(storeId, templateCode);

        // 6. Opt-in — marketing requires explicit consent
        if (!optInGuard.allowed(customer, template.getCategory())) {
            return skip(
                    storeId,
                    customerId,
                    template.getId(),
                    recipientPhone(customer),
                    MessageLog.MessageStatus.SKIPPED_NOT_OPTED_IN,
                    "Customer not opted in to marketing",
                    sentBy,
                    refType,
                    refId,
                    campaignId);
        }

        // 7. Render + persist QUEUED log
        TemplateResolver.RenderedMessage rendered = templateResolver.render(template, variables);
        String phone = recipientPhone(customer);

        MessageLog logRow =
                MessageLog.builder()
                        .storeId(storeId)
                        .customerId(customerId)
                        .templateId(template.getId())
                        .channel(MessageChannel.WHATSAPP)
                        .recipientPhone(phone)
                        .recipientName(fullName(customer))
                        .messageBody(rendered.text())
                        .mediaUrl(mediaUrl)
                        .provider("META")
                        .status(MessageLog.MessageStatus.QUEUED)
                        .refType(refType)
                        .refId(refId)
                        .campaignId(campaignId)
                        .sentBy(sentBy)
                        .build();
        logRow.setActive(true);
        logRow = messageLogRepository.save(logRow);

        // 8. Dispatch (synchronous in M2 — no DB transaction is held across this call)
        WhatsAppTemplateSend send =
                new WhatsAppTemplateSend(
                        phone,
                        template.getMetaTemplateName(),
                        template.getMetaLanguage(),
                        template.getHeaderType(),
                        mediaUrl,
                        rendered.params());

        SendResult result = whatsAppProvider.sendTemplate(waba, send);

        if (result.ok()) {
            logRow.setStatus(MessageLog.MessageStatus.SENT);
            logRow.setProviderMessageId(result.providerMessageId());
        } else {
            logRow.setStatus(MessageLog.MessageStatus.FAILED);
            logRow.setFailureReason(result.error());
        }
        logRow = messageLogRepository.save(logRow);

        return MessageResponse.builder()
                .messageLogId(logRow.getId())
                .status(logRow.getStatus().name())
                .providerMessageId(logRow.getProviderMessageId())
                .build();
    }

    /** Record a send that was intentionally not dispatched, and return its outcome. */
    private MessageResponse skip(
            Long storeId,
            Long customerId,
            Long templateId,
            String phone,
            MessageLog.MessageStatus status,
            String reason,
            Long sentBy,
            String refType,
            Long refId,
            Long campaignId) {
        MessageLog logRow =
                MessageLog.builder()
                        .storeId(storeId)
                        .customerId(customerId)
                        .templateId(templateId)
                        .channel(MessageChannel.WHATSAPP)
                        .recipientPhone(phone != null ? phone : "")
                        .status(status)
                        .failureReason(reason)
                        .provider("META")
                        .refType(refType)
                        .refId(refId)
                        .campaignId(campaignId)
                        .sentBy(sentBy)
                        .build();
        logRow.setActive(true);
        logRow = messageLogRepository.save(logRow);
        log.info("Message skipped store={} customer={} status={}", storeId, customerId, status);
        return MessageResponse.builder().messageLogId(logRow.getId()).status(status.name()).build();
    }

    private String fullName(Customer c) {
        String last = c.getLastName();
        return last != null && !last.isBlank() ? c.getFirstName() + " " + last : c.getFirstName();
    }

    private String recipientPhone(Customer c) {
        return toE164(c.getPhone());
    }

    /** Normalise an Indian mobile number to E.164 digits (no +), defaulting the 91 country code. */
    static String toE164(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return "91" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return "91" + digits.substring(1);
        }
        return digits;
    }
}
