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
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.CustomerEnquiry;
import com.aurajewels.jewel.entity.MessageLog;
import com.aurajewels.jewel.entity.StoreWaba;
import com.aurajewels.jewel.repository.CustomerEnquiryRepository;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.MessageCampaignRepository;
import com.aurajewels.jewel.repository.MessageLogRepository;
import com.aurajewels.jewel.repository.StoreWabaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes Meta WhatsApp webhook callbacks: the GET verification handshake, delivery/read status
 * updates, and inbound customer messages (routed to the owning store and turned into an enquiry
 * when the sender is a known customer). All processing is best-effort — the controller always
 * returns 200 so Meta does not retry.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookService {

    private static final String PLACEHOLDER = "placeholder";

    private final WhatsAppProperties properties;
    private final ObjectMapper objectMapper;
    private final MessageLogRepository messageLogRepository;
    private final StoreWabaRepository storeWabaRepository;
    private final CustomerRepository customerRepository;
    private final CustomerEnquiryRepository customerEnquiryRepository;
    private final MessageCampaignRepository campaignRepository;

    /** Meta GET verification: echo the challenge when the verify token matches. */
    public Optional<String> verifySubscription(String mode, String verifyToken, String challenge) {
        String expected = properties.getMeta().getWebhookVerifyToken();
        if ("subscribe".equals(mode) && expected != null && expected.equals(verifyToken)) {
            return Optional.ofNullable(challenge);
        }
        log.warn("WhatsApp webhook verification failed (mode={})", mode);
        return Optional.empty();
    }

    /** Verify the payload signature, then dispatch status + inbound events. */
    @Transactional
    public void handleEvent(String payload, String signatureHeader) {
        if (!signatureValid(payload, signatureHeader)) {
            log.warn("WhatsApp webhook signature invalid — ignoring payload");
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            for (JsonNode entry : root.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    JsonNode value = change.path("value");
                    for (JsonNode status : value.path("statuses")) {
                        applyStatus(status);
                    }
                    String phoneNumberId =
                            value.path("metadata").path("phone_number_id").asText(null);
                    for (JsonNode message : value.path("messages")) {
                        applyInbound(phoneNumberId, message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process WhatsApp webhook: {}", e.getMessage(), e);
        }
    }

    // ── Status updates ──────────────────────────────────────────────

    private void applyStatus(JsonNode status) {
        String wamid = status.path("id").asText(null);
        String state = status.path("status").asText(null);
        if (wamid == null || state == null) {
            return;
        }
        MessageLog logRow = messageLogRepository.findByProviderMessageId(wamid).orElse(null);
        if (logRow == null) {
            return;
        }
        Instant ts = parseTimestamp(status.path("timestamp").asText(null));

        switch (state) {
            case "sent" -> advance(logRow, MessageLog.MessageStatus.SENT);
            case "delivered" -> {
                if (advance(logRow, MessageLog.MessageStatus.DELIVERED)) {
                    logRow.setDeliveredAt(ts);
                    bumpCampaign(logRow, MessageLog.MessageStatus.DELIVERED);
                }
            }
            case "read" -> {
                if (advance(logRow, MessageLog.MessageStatus.READ)) {
                    logRow.setReadAt(ts);
                    bumpCampaign(logRow, MessageLog.MessageStatus.READ);
                }
            }
            case "failed" -> {
                if (logRow.getStatus() != MessageLog.MessageStatus.READ) {
                    logRow.setStatus(MessageLog.MessageStatus.FAILED);
                    logRow.setFailureReason(
                            status.path("errors").path(0).path("title").asText("delivery failed"));
                }
            }
            default -> {
                return;
            }
        }
        messageLogRepository.save(logRow);
    }

    /** Increment the owning campaign's delivered/read counter when a campaign message advances. */
    private void bumpCampaign(MessageLog logRow, MessageLog.MessageStatus target) {
        if (logRow.getCampaignId() == null) {
            return;
        }
        campaignRepository
                .findById(logRow.getCampaignId())
                .ifPresent(
                        campaign -> {
                            if (target == MessageLog.MessageStatus.DELIVERED) {
                                campaign.setDeliveredCount(nz(campaign.getDeliveredCount()) + 1);
                            } else if (target == MessageLog.MessageStatus.READ) {
                                campaign.setReadCount(nz(campaign.getReadCount()) + 1);
                            }
                            campaignRepository.save(campaign);
                        });
    }

    private int nz(Integer value) {
        return value != null ? value : 0;
    }

    /** Only ever move a message forward through QUEUED → SENT → DELIVERED → READ. */
    private boolean advance(MessageLog logRow, MessageLog.MessageStatus target) {
        if (rank(target) > rank(logRow.getStatus())) {
            logRow.setStatus(target);
            return true;
        }
        return false;
    }

    private int rank(MessageLog.MessageStatus status) {
        if (status == null) {
            return -1;
        }
        return switch (status) {
            case QUEUED -> 0;
            case SENT -> 1;
            case DELIVERED -> 2;
            case READ -> 3;
            default -> -1;
        };
    }

    // ── Inbound messages ────────────────────────────────────────────

    private void applyInbound(String phoneNumberId, JsonNode message) {
        if (phoneNumberId == null) {
            return;
        }
        StoreWaba waba = storeWabaRepository.findByPhoneNumberId(phoneNumberId).orElse(null);
        if (waba == null || waba.getStore() == null) {
            log.warn("Inbound WhatsApp for unknown phone_number_id {}", phoneNumberId);
            return;
        }
        Long storeId = waba.getStore().getId();
        String from = message.path("from").asText(null);
        String text = extractText(message);

        Customer customer = findCustomer(storeId, from).orElse(null);
        if (customer == null) {
            log.info("Inbound WhatsApp from unknown number {} for store {}", from, storeId);
            return;
        }

        // Inbound proves the number is reachable on WhatsApp and opens the 24h service window.
        customer.setHasWhatsapp(true);
        customerRepository.save(customer);

        CustomerEnquiry enquiry =
                CustomerEnquiry.builder()
                        .customer(customer)
                        .store(customer.getStore())
                        .subject("WhatsApp message")
                        .message(text != null ? text : "(non-text message)")
                        .status(CustomerEnquiry.EnquiryStatus.OPEN)
                        .build();
        customerEnquiryRepository.save(enquiry);
        log.info(
                "Created enquiry from inbound WhatsApp: customer={} store={}",
                customer.getId(),
                storeId);
    }

    private String extractText(JsonNode message) {
        String type = message.path("type").asText("");
        if ("text".equals(type)) {
            return message.path("text").path("body").asText(null);
        }
        return "[" + type + "] message";
    }

    /** Match the WhatsApp sender (E.164 digits) to a store customer, tolerating the 91 prefix. */
    private Optional<Customer> findCustomer(Long storeId, String waPhone) {
        if (waPhone == null) {
            return Optional.empty();
        }
        String digits = waPhone.replaceAll("\\D", "");
        Optional<Customer> byFull = customerRepository.findByPhoneAndStoreId(digits, storeId);
        if (byFull.isPresent()) {
            return byFull;
        }
        if (digits.length() > 10) {
            String local = digits.substring(digits.length() - 10);
            return customerRepository.findByPhoneAndStoreId(local, storeId);
        }
        return Optional.empty();
    }

    // ── Signature ───────────────────────────────────────────────────

    private boolean signatureValid(String payload, String signatureHeader) {
        String appSecret = properties.getMeta().getAppSecret();
        // In local/dev the secret is a placeholder — skip verification to allow testing.
        if (appSecret == null || appSecret.isBlank() || PLACEHOLDER.equals(appSecret)) {
            log.warn("Meta app secret not configured — skipping webhook signature verification");
            return true;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String expected = "sha256=" + hmacSha256Hex(appSecret, payload);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signatureHeader.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit((b & 0xF), 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute webhook signature", e);
        }
    }

    private Instant parseTimestamp(String epochSeconds) {
        try {
            return epochSeconds != null
                    ? Instant.ofEpochSecond(Long.parseLong(epochSeconds))
                    : Instant.now();
        } catch (NumberFormatException e) {
            return Instant.now();
        }
    }
}
