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
 * A message template mapped to a Meta-approved WhatsApp template. A row with a null store is a
 * platform-level default; a row with a store overrides the default for that store. The {@code
 * buttons} and {@code variables} columns hold serialized JSON.
 *
 * @author Raviraj Bhosale
 */
@Entity
@Table(name = "message_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageTemplate extends BaseEntity {

    /** Null = platform default template shared by all stores. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Store store;

    @Column(name = "code", nullable = false, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel")
    private MessageChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private TemplateCategory category;

    @Column(name = "meta_template_name", length = 120)
    private String metaTemplateName;

    @Column(name = "meta_language", length = 10)
    private String metaLanguage;

    @Enumerated(EnumType.STRING)
    @Column(name = "header_type")
    private HeaderType headerType;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    /** Serialized JSON array of button descriptors: [{type,text,url}]. */
    @Column(name = "buttons", columnDefinition = "TEXT")
    private String buttons;

    /** Serialized JSON array of ordered variable names. */
    @Column(name = "variables", columnDefinition = "TEXT")
    private String variables;

    public enum TemplateCategory {
        UTILITY,
        MARKETING,
        AUTHENTICATION
    }

    public enum HeaderType {
        NONE,
        TEXT,
        IMAGE,
        DOCUMENT
    }
}
