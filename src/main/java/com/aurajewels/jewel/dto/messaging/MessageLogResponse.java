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
package com.aurajewels.jewel.dto.messaging;

import com.aurajewels.jewel.entity.MessageLog;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A message log row for history listings.
 *
 * @author Raviraj Bhosale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageLogResponse {

    private Long id;
    private Long customerId;
    private Long campaignId;
    private String recipientPhone;
    private String recipientName;
    private String channel;
    private String status;
    private String messageBody;
    private String providerMessageId;
    private String failureReason;
    private Instant createdAt;

    public static MessageLogResponse from(MessageLog log) {
        return MessageLogResponse.builder()
                .id(log.getId())
                .customerId(log.getCustomerId())
                .campaignId(log.getCampaignId())
                .recipientPhone(log.getRecipientPhone())
                .recipientName(log.getRecipientName())
                .channel(log.getChannel() != null ? log.getChannel().name() : null)
                .status(log.getStatus() != null ? log.getStatus().name() : null)
                .messageBody(log.getMessageBody())
                .providerMessageId(log.getProviderMessageId())
                .failureReason(log.getFailureReason())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
