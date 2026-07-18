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
 * A bulk marketing send to a resolved audience (all customers, birthdays today, scheme members,
 * etc.). Counters are updated as sends are dispatched and as delivery webhooks arrive. The {@code
 * audienceFilter} and {@code variables} columns hold serialized JSON.
 *
 * @author Raviraj Bhosale
 */
@Entity
@Table(name = "message_campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageCampaign extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Store store;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private MessageTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false)
    private AudienceType audienceType;

    /** Serialized JSON — extra audience filter criteria (e.g. explicit customer id list). */
    @Column(name = "audience_filter", columnDefinition = "TEXT")
    private String audienceFilter;

    /** Serialized JSON — variable values applied to every recipient. */
    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(name = "total_recipients")
    private Integer totalRecipients;

    @Column(name = "sent_count")
    private Integer sentCount;

    @Column(name = "delivered_count")
    private Integer deliveredCount;

    @Column(name = "read_count")
    private Integer readCount;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CampaignStatus status;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_by")
    private Long createdBy;

    public enum AudienceType {
        ALL_CUSTOMERS,
        BIRTHDAY_TODAY,
        ANNIVERSARY_TODAY,
        SCHEME_MEMBERS,
        CUSTOM_LIST
    }

    public enum CampaignStatus {
        DRAFT,
        QUEUED,
        SENDING,
        COMPLETED,
        FAILED
    }
}
