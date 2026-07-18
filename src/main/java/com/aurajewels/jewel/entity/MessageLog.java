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
package com.aurajewels.jewel.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Audit + delivery-tracking record for every message send attempt. A row is written with status
 * QUEUED before the provider call, then updated to SENT/FAILED after the call and to DELIVERED/READ
 * by the delivery webhook. SKIPPED_* statuses record sends that were intentionally not attempted
 * (feature off, no channel, not opted in).
 *
 * <p>For auto-triggered sends the {@code refType}/{@code refId} pair (e.g. INVOICE + invoice id)
 * combined with the unique key {@code uk_mlog_idem} guarantees a message is sent at most once.
 *
 * @author Raviraj Bhosale
 */
@Entity
@Table(name = "message_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageLog extends BaseEntity {

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "template_id")
    private Long templateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private MessageChannel channel;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String recipientPhone;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "provider", length = 20)
    private String provider;

    /** Provider message id (Meta wamid) — used to match delivery-status webhooks. */
    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Column(name = "conversation_id", length = 120)
    private String conversationId;

    @Column(name = "conversation_category", length = 20)
    private String conversationCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private MessageStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Source entity type for auto-triggered sends, e.g. INVOICE / SCHEME_DUE / CUSTOM_ORDER. */
    @Column(name = "ref_type", length = 40)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "sent_by")
    private Long sentBy;

    public enum MessageStatus {
        QUEUED,
        SENT,
        DELIVERED,
        READ,
        FAILED,
        SKIPPED_NO_CHANNEL,
        SKIPPED_NOT_OPTED_IN,
        SKIPPED_FEATURE_OFF
    }
}
