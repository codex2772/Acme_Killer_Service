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

import com.aurajewels.jewel.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;

/**
 * Production {@link WabaTokenStore} backed by AWS Secrets Manager. Each store's token is stored
 * under {@code <tokenSecretPrefix>/<storeId>}; only that secret name is persisted on the WABA
 * record ({@code store_waba.access_token_ref}).
 *
 * <p>Activated with {@code whatsapp.token-store=secrets-manager}. Requires the ECS task role to
 * have {@code secretsmanager:CreateSecret}, {@code PutSecretValue} and {@code GetSecretValue} on
 * the {@code <prefix>/*} secrets.
 *
 * @author Raviraj Bhosale
 */
@Component
@ConditionalOnProperty(name = "whatsapp.token-store", havingValue = "secrets-manager")
@RequiredArgsConstructor
@Slf4j
public class SecretsManagerWabaTokenStore implements WabaTokenStore {

    private final SecretsManagerClient secretsManagerClient;
    private final WhatsAppProperties properties;

    @Override
    public String store(Long storeId, String accessToken) {
        String secretName = properties.getTokenSecretPrefix() + "/" + storeId;
        try {
            secretsManagerClient.createSecret(
                    CreateSecretRequest.builder()
                            .name(secretName)
                            .secretString(accessToken)
                            .description("WhatsApp access token for store " + storeId)
                            .build());
            log.info("Created WhatsApp token secret for store {}", storeId);
        } catch (ResourceExistsException e) {
            secretsManagerClient.putSecretValue(
                    PutSecretValueRequest.builder()
                            .secretId(secretName)
                            .secretString(accessToken)
                            .build());
            log.info("Rotated WhatsApp token secret for store {}", storeId);
        }
        return secretName;
    }

    @Override
    public String resolve(String tokenRef) {
        return secretsManagerClient
                .getSecretValue(GetSecretValueRequest.builder().secretId(tokenRef).build())
                .secretString();
    }
}
