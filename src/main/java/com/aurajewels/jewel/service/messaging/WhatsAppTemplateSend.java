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

import com.aurajewels.jewel.entity.MessageTemplate;
import java.util.List;

/**
 * Provider-agnostic description of a template message to dispatch.
 *
 * @param toPhone recipient in E.164 digits (no +), e.g. 919999999999
 * @param metaTemplateName template name as registered/approved with the provider
 * @param languageCode template language code, e.g. en / en_US
 * @param headerType header format, drives whether {@code mediaUrl} is attached
 * @param mediaUrl link to header image/document (used only for IMAGE/DOCUMENT headers)
 * @param bodyParams ordered values substituted into the template body placeholders
 * @author Raviraj Bhosale
 */
public record WhatsAppTemplateSend(
        String toPhone,
        String metaTemplateName,
        String languageCode,
        MessageTemplate.HeaderType headerType,
        String mediaUrl,
        List<String> bodyParams) {}
