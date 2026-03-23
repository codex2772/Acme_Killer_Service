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
import lombok.*;

/**
 * JPA entity representing an AR overlay asset linked to a jewelry item for virtual try-on.
 *
 * @author Raviraj Bhosale
 */
@Entity
@Table(name = "ar_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArAsset extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jewelry_item_id", nullable = false)
    private JewelryItem jewelryItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "ar_type", nullable = false)
    private ArType arType;

    @Column(name = "overlay_url", length = 500)
    private String overlayUrl;

    @Column(name = "model_url", length = 500)
    private String modelUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "anchor_config", columnDefinition = "JSON")
    private String anchorConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ArAssetStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private ArAssetSource source;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    /** AR jewelry type — determines which body part to anchor the overlay to. */
    public enum ArType {
        NECKLACE,
        CHOKER,
        EARRING,
        RING,
        BANGLE,
        BRACELET,
        MAANG_TIKKA,
        NOSE_RING,
        ANKLET,
        PENDANT,
        CHAIN
    }

    /** Processing status of the AR asset. */
    public enum ArAssetStatus {
        PENDING,
        READY,
        FAILED,
        DISABLED
    }

    /** How the AR overlay was created. */
    public enum ArAssetSource {
        MANUAL_UPLOAD,
        AUTO_GENERATED,
        PHOTO_PROCESSED
    }
}
