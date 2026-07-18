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
 * Per-store WhatsApp Business Account (WABA) connection. Each store owns its own WhatsApp number,
 * display name and access token so messages are branded per store and quality ratings stay
 * isolated. The access token itself lives in AWS Secrets Manager — only its reference key is stored
 * here.
 *
 * @author Raviraj Bhosale
 */
@Entity
@Table(name = "store_waba")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreWaba extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Store store;

    @Column(name = "waba_id", length = 64)
    private String wabaId;

    @Column(name = "phone_number_id", length = 64)
    private String phoneNumberId;

    @Column(name = "display_number", length = 20)
    private String displayNumber;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WabaStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_rating")
    private QualityRating qualityRating;

    @Column(name = "messaging_tier", length = 20)
    private String messagingTier;

    /** Opaque token reference (encrypted token or secret name) — never a plaintext token. */
    @Column(name = "access_token_ref", columnDefinition = "TEXT")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String accessTokenRef;

    @Column(name = "provider", length = 20)
    private String provider;

    @Column(name = "connected_at")
    private Instant connectedAt;

    public enum WabaStatus {
        PENDING,
        CONNECTED,
        SUSPENDED,
        DISCONNECTED
    }

    public enum QualityRating {
        HIGH,
        MEDIUM,
        LOW,
        UNKNOWN
    }
}
