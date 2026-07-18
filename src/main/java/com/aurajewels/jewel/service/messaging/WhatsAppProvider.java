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

import com.aurajewels.jewel.entity.StoreWaba;

/**
 * Abstraction over a WhatsApp Business API provider. The only implementation today is {@link
 * MetaCloudWhatsAppProvider} (Meta Cloud API direct); a future MSG91-backed implementation can be
 * dropped in behind this interface with no change to the calling services.
 *
 * @author Raviraj Bhosale
 */
public interface WhatsAppProvider {

    /**
     * Send a pre-approved template message from the given store's WhatsApp number.
     *
     * @param waba the sending store's connected WhatsApp Business Account
     * @param send the resolved template payload
     * @return the send outcome (never throws for expected provider errors)
     */
    SendResult sendTemplate(StoreWaba waba, WhatsAppTemplateSend send);
}
