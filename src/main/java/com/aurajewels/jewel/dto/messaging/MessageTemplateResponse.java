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

import com.aurajewels.jewel.entity.MessageTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A message template exposed to store staff.
 *
 * @author Raviraj Bhosale
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplateResponse {

    private Long id;
    private String code;
    private String name;
    private String channel;
    private String category;
    private String headerType;
    private String bodyTemplate;
    private String variables;
    private boolean platformDefault;

    public static MessageTemplateResponse from(MessageTemplate t) {
        return MessageTemplateResponse.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .channel(t.getChannel() != null ? t.getChannel().name() : null)
                .category(t.getCategory() != null ? t.getCategory().name() : null)
                .headerType(t.getHeaderType() != null ? t.getHeaderType().name() : null)
                .bodyTemplate(t.getBodyTemplate())
                .variables(t.getVariables())
                .platformDefault(t.getStore() == null)
                .build();
    }
}
