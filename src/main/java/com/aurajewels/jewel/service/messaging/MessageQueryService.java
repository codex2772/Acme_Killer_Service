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

import com.aurajewels.jewel.dto.messaging.MessageLogResponse;
import com.aurajewels.jewel.dto.messaging.MessageStatsResponse;
import com.aurajewels.jewel.dto.messaging.MessageTemplateResponse;
import com.aurajewels.jewel.dto.messaging.OptInRequest;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.MessageLog;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.MessageLogRepository;
import com.aurajewels.jewel.repository.MessageTemplateRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read models and small mutations for the messaging module: templates, logs, monthly usage stats
 * and customer opt-in capture.
 *
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class MessageQueryService {

    private final MessageLogRepository messageLogRepository;
    private final MessageTemplateRepository messageTemplateRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<MessageTemplateResponse> templates(Long storeId) {
        return messageTemplateRepository.findVisibleToStore(storeId).stream()
                .map(MessageTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MessageLogResponse> recentLogs(Long storeId, int limit) {
        return messageLogRepository
                .findByStoreIdOrderByCreatedAtDesc(storeId, PageRequest.of(0, limit))
                .map(MessageLogResponse::from)
                .getContent();
    }

    /** Usage snapshot for the current calendar month, bucketed by each message's current status. */
    @Transactional(readOnly = true)
    public MessageStatsResponse monthStats(Long storeId) {
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        Instant since = firstOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
        return MessageStatsResponse.builder()
                .periodStart(firstOfMonth.toString())
                .total(messageLogRepository.countByStoreIdAndCreatedAtAfter(storeId, since))
                .sent(count(storeId, MessageLog.MessageStatus.SENT, since))
                .delivered(count(storeId, MessageLog.MessageStatus.DELIVERED, since))
                .read(count(storeId, MessageLog.MessageStatus.READ, since))
                .failed(count(storeId, MessageLog.MessageStatus.FAILED, since))
                .build();
    }

    @Transactional
    public void setOptIn(Long storeId, OptInRequest request) {
        Customer customer =
                customerRepository
                        .findByIdAndStoreId(request.getCustomerId(), storeId)
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "Customer not found: " + request.getCustomerId()));
        customer.setWhatsappOptIn(request.isOptIn());
        if (request.isOptIn()) {
            customer.setWhatsappOptInSource(request.getSource());
            customer.setWhatsappOptInAt(Instant.now());
        }
        customerRepository.save(customer);
    }

    private long count(Long storeId, MessageLog.MessageStatus status, Instant since) {
        return messageLogRepository.countByStoreIdAndStatusAndCreatedAtAfter(
                storeId, status, since);
    }
}
