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
package com.aurajewels.jewel.controller;

import com.aurajewels.jewel.dto.messaging.BulkMessageRequest;
import com.aurajewels.jewel.dto.messaging.CampaignResponse;
import com.aurajewels.jewel.dto.messaging.MessageLogResponse;
import com.aurajewels.jewel.dto.messaging.MessageResponse;
import com.aurajewels.jewel.dto.messaging.MessageStatsResponse;
import com.aurajewels.jewel.dto.messaging.MessageTemplateResponse;
import com.aurajewels.jewel.dto.messaging.OptInRequest;
import com.aurajewels.jewel.dto.messaging.SendMessageRequest;
import com.aurajewels.jewel.security.RequiresModule;
import com.aurajewels.jewel.security.RequiresPermission;
import com.aurajewels.jewel.security.StoreContext;
import com.aurajewels.jewel.service.messaging.MessageCampaignService;
import com.aurajewels.jewel.service.messaging.MessageQueryService;
import com.aurajewels.jewel.service.messaging.MessagingService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for customer messaging. Guarded by the WHATSAPP feature module.
 *
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessagingService messagingService;
    private final MessageCampaignService campaignService;
    private final MessageQueryService queryService;

    /** POST /api/messages/send — send a single templated WhatsApp message to a customer. */
    @PostMapping("/send")
    @RequiresPermission("MANAGE_CUSTOMERS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<MessageResponse> send(@RequestBody SendMessageRequest request) {
        MessageResponse response =
                messagingService.sendTemplated(
                        StoreContext.getCurrentStoreId(),
                        request.getCustomerId(),
                        request.getTemplateCode(),
                        request.getVariables(),
                        request.getMediaUrl(),
                        null,
                        null,
                        StoreContext.getCurrentUserId());
        return ResponseEntity.ok(response);
    }

    /** POST /api/messages/bulk — run a campaign to a resolved audience. */
    @PostMapping("/bulk")
    @RequiresPermission("MANAGE_CUSTOMERS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<CampaignResponse> bulk(@RequestBody BulkMessageRequest request) {
        return ResponseEntity.ok(
                campaignService.createAndSend(
                        StoreContext.getCurrentStoreId(),
                        request,
                        StoreContext.getCurrentUserId()));
    }

    /** GET /api/messages/campaigns — recent campaigns for the store. */
    @GetMapping("/campaigns")
    @RequiresPermission("VIEW_REPORTS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<List<CampaignResponse>> campaigns(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(
                campaignService.listCampaigns(StoreContext.getCurrentStoreId(), limit));
    }

    /** GET /api/messages/campaigns/{id} — a campaign with live counters. */
    @GetMapping("/campaigns/{id}")
    @RequiresPermission("VIEW_REPORTS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<CampaignResponse> campaign(@PathVariable Long id) {
        return ResponseEntity.ok(campaignService.getCampaign(StoreContext.getCurrentStoreId(), id));
    }

    /** GET /api/messages/templates — templates visible to the store. */
    @GetMapping("/templates")
    @RequiresPermission("MANAGE_CUSTOMERS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<List<MessageTemplateResponse>> templates() {
        return ResponseEntity.ok(queryService.templates(StoreContext.getCurrentStoreId()));
    }

    /** GET /api/messages/logs — recent message history. */
    @GetMapping("/logs")
    @RequiresPermission("VIEW_REPORTS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<List<MessageLogResponse>> logs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(queryService.recentLogs(StoreContext.getCurrentStoreId(), limit));
    }

    /** GET /api/messages/stats — current-month usage snapshot. */
    @GetMapping("/stats")
    @RequiresPermission("VIEW_REPORTS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<MessageStatsResponse> stats() {
        return ResponseEntity.ok(queryService.monthStats(StoreContext.getCurrentStoreId()));
    }

    /** PUT /api/messages/opt-in — capture or revoke a customer's WhatsApp marketing consent. */
    @PutMapping("/opt-in")
    @RequiresPermission("MANAGE_CUSTOMERS")
    @RequiresModule("WHATSAPP")
    public ResponseEntity<Map<String, Object>> optIn(@RequestBody OptInRequest request) {
        queryService.setOptIn(StoreContext.getCurrentStoreId(), request);
        return ResponseEntity.ok(
                Map.of("customerId", request.getCustomerId(), "optIn", request.isOptIn()));
    }
}
