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
import com.aurajewels.jewel.entity.StoreWaba;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link WhatsAppProvider} implementation calling Meta's WhatsApp Cloud API directly (no BSP). The
 * per-store bearer token is resolved from the {@link WabaTokenStore} for each send.
 *
 * <p>Active by default; a future MSG91 provider can be selected via {@code whatsapp.provider}.
 *
 * @author Raviraj Bhosale
 */
@Component
@ConditionalOnProperty(name = "whatsapp.provider", havingValue = "meta", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class MetaCloudWhatsAppProvider implements WhatsAppProvider {

    private final RestClient metaRestClient;
    private final WabaTokenStore tokenStore;

    private static final String FALLBACK_LANGUAGE = "en_US";

    @Override
    public SendResult sendTemplate(StoreWaba waba, WhatsAppTemplateSend send) {
        final String token;
        try {
            token = tokenStore.resolve(waba.getAccessTokenRef());
        } catch (Exception e) {
            log.error("Failed to resolve WhatsApp access token: {}", e.getMessage(), e);
            return SendResult.fail("Access token unavailable: " + e.getMessage());
        }

        String language =
                send.languageCode() != null && !send.languageCode().isBlank()
                        ? send.languageCode()
                        : "en";

        SendResult result = post(waba, send, token, language);

        // Many templates are approved under en_US rather than en — retry once on a
        // language/translation-not-found error before giving up.
        if (!result.ok()
                && !FALLBACK_LANGUAGE.equalsIgnoreCase(language)
                && isLanguageMismatch(result.error())) {
            log.info(
                    "Template '{}' not found in language '{}'; retrying as {}",
                    send.metaTemplateName(),
                    language,
                    FALLBACK_LANGUAGE);
            result = post(waba, send, token, FALLBACK_LANGUAGE);
        }
        return result;
    }

    private SendResult post(
            StoreWaba waba, WhatsAppTemplateSend send, String token, String languageCode) {
        try {
            Map<String, Object> payload = buildTemplatePayload(send, languageCode);

            JsonNode response =
                    metaRestClient
                            .post()
                            .uri("/{phoneNumberId}/messages", waba.getPhoneNumberId())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(JsonNode.class);

            String messageId =
                    response != null
                            ? response.path("messages").path(0).path("id").asText(null)
                            : null;
            if (messageId == null || messageId.isBlank()) {
                return SendResult.fail("No message id in Meta response");
            }
            return SendResult.ok(messageId);

        } catch (RestClientResponseException e) {
            String reason = extractError(e.getResponseBodyAsString());
            log.warn(
                    "Meta WhatsApp send failed for store {} ({}): {}",
                    waba.getStore() != null ? waba.getStore().getId() : null,
                    e.getStatusCode(),
                    reason);
            return SendResult.fail(reason);
        } catch (Exception e) {
            log.error("Unexpected error sending WhatsApp template: {}", e.getMessage(), e);
            return SendResult.fail(e.getMessage());
        }
    }

    /** True when a send error indicates the template is not available in the requested language. */
    static boolean isLanguageMismatch(String error) {
        if (error == null) {
            return false;
        }
        String lower = error.toLowerCase();
        return lower.contains("132001")
                || (lower.contains("does not exist")
                        && (lower.contains("translation")
                                || lower.contains("language")
                                || lower.contains("locale")));
    }

    private Map<String, Object> buildTemplatePayload(
            WhatsAppTemplateSend send, String languageCode) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("name", send.metaTemplateName());
        template.put("language", Map.of("code", languageCode));

        List<Map<String, Object>> components = new ArrayList<>();

        boolean hasMediaHeader =
                send.mediaUrl() != null
                        && (send.headerType() == MessageTemplate.HeaderType.IMAGE
                                || send.headerType() == MessageTemplate.HeaderType.DOCUMENT);
        if (hasMediaHeader) {
            String mediaType =
                    send.headerType() == MessageTemplate.HeaderType.IMAGE ? "image" : "document";
            Map<String, Object> mediaParam = new LinkedHashMap<>();
            mediaParam.put("type", mediaType);
            mediaParam.put(mediaType, Map.of("link", send.mediaUrl()));
            components.add(Map.of("type", "header", "parameters", List.of(mediaParam)));
        }

        if (send.bodyParams() != null && !send.bodyParams().isEmpty()) {
            List<Map<String, Object>> params = new ArrayList<>();
            for (String v : send.bodyParams()) {
                params.add(Map.of("type", "text", "text", v != null ? v : ""));
            }
            components.add(Map.of("type", "body", "parameters", params));
        }

        if (!components.isEmpty()) {
            template.put("components", components);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", send.toPhone());
        payload.put("type", "template");
        payload.put("template", template);
        return payload;
    }

    /**
     * Pull the human-readable message out of Meta's error envelope, falling back to the raw body.
     */
    private String extractError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Unknown Meta API error";
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode node = mapper.readTree(responseBody);
            JsonNode error = node.path("error");
            String message = error.path("message").asText(null);
            String details = error.path("error_data").path("details").asText(null);
            if (message != null) {
                return details != null ? message + " — " + details : message;
            }
        } catch (Exception ignored) {
            // fall through to raw body
        }
        return responseBody.length() > 480 ? responseBody.substring(0, 480) : responseBody;
    }
}
