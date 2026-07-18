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

/**
 * Secure storage for per-store WhatsApp access tokens. Tokens never live in the application
 * database — only the opaque reference returned by {@link #store} is persisted (on {@code
 * store_waba.access_token_ref}).
 *
 * @author Raviraj Bhosale
 */
public interface WabaTokenStore {

    /**
     * Persist a store's access token and return the reference used to resolve it later.
     *
     * @param storeId owning store
     * @param accessToken the raw provider access token
     * @return an opaque reference to persist on the WABA record
     */
    String store(Long storeId, String accessToken);

    /**
     * Resolve the raw access token for a stored reference.
     *
     * @param tokenRef the reference previously returned by {@link #store}
     * @return the raw access token
     */
    String resolve(String tokenRef);
}
