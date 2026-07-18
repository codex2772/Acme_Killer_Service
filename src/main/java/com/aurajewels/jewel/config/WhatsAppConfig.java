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
package com.aurajewels.jewel.config;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * WhatsApp integration configuration: the Meta Graph API HTTP client and the executor used for
 * off-request message dispatch.
 *
 * <p>{@link EnableAsync} activates {@code @Async} across the application so message sends (and
 * other fire-and-forget work) run off the request thread.
 *
 * @author Raviraj Bhosale
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class WhatsAppConfig {

    private final WhatsAppProperties properties;

    /**
     * Base client for Meta's WhatsApp Cloud API. The per-store bearer token is applied per request
     * (tokens differ per store), so no Authorization header is set on the shared client.
     */
    @Bean
    public RestClient metaRestClient() {
        return RestClient.builder().baseUrl(properties.getMeta().getGraphBaseUrl()).build();
    }

    /**
     * AWS Secrets Manager client for the secrets-manager token store. Only created when {@code
     * whatsapp.token-store=secrets-manager}, so the default (local) store needs no AWS credentials.
     */
    @Bean
    @ConditionalOnProperty(name = "whatsapp.token-store", havingValue = "secrets-manager")
    public SecretsManagerClient secretsManagerClient(@Value("${aws.s3.region}") String region) {
        return SecretsManagerClient.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Dedicated executor for message dispatch, referenced via {@code @Async("messagingExecutor")}.
     */
    @Bean(name = "messagingExecutor")
    public Executor messagingExecutor(@Value("${messaging.async-pool-size:4}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("messaging-");
        executor.initialize();
        return executor;
    }
}
