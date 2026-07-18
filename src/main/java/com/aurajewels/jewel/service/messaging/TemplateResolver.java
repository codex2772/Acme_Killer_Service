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
import com.aurajewels.jewel.repository.MessageTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Resolves a message template by code (store override takes precedence over the platform default)
 * and renders its body placeholders from a variable map.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateResolver {

    private final MessageTemplateRepository messageTemplateRepository;
    private final ObjectMapper objectMapper;

    /** A store-scoped template wins; otherwise the platform default (store is null) is used. */
    public MessageTemplate resolve(Long storeId, String code) {
        return messageTemplateRepository
                .findByStore_IdAndCode(storeId, code)
                .or(() -> messageTemplateRepository.findByStoreIsNullAndCode(code))
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "No message template found for code: " + code));
    }

    /**
     * Substitute {@code {{1}}, {{2}} ...} placeholders in the template body using values pulled
     * from {@code variables}, keyed by the template's ordered variable names.
     */
    public RenderedMessage render(MessageTemplate template, Map<String, Object> variables) {
        List<String> names = parseVariableNames(template.getVariables());
        List<String> params = new ArrayList<>(names.size());
        for (String name : names) {
            Object value = variables != null ? variables.get(name) : null;
            params.add(value != null ? String.valueOf(value) : "");
        }

        String text = template.getBodyTemplate();
        for (int i = 0; i < params.size(); i++) {
            text = text.replace("{{" + (i + 1) + "}}", params.get(i));
        }
        return new RenderedMessage(text, params);
    }

    private List<String> parseVariableNames(String variablesJson) {
        if (variablesJson == null || variablesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(variablesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse template variables JSON: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Rendered message ready to dispatch.
     *
     * @param text the fully substituted body (for logging/preview)
     * @param params ordered body parameter values for the provider template call
     */
    public record RenderedMessage(String text, List<String> params) {}
}
