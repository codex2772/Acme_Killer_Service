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
package com.aurajewels.jewel.controller.admin;

import com.aurajewels.jewel.dto.admin.OnboardOrgRequest;
import com.aurajewels.jewel.dto.admin.OnboardOrgResponse;
import com.aurajewels.jewel.entity.Organization;
import com.aurajewels.jewel.service.admin.OrganizationAdminService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class OrganizationAdminController {

    private final OrganizationAdminService organizationAdminService;

    @GetMapping
    public ResponseEntity<List<Organization>> listOrganizations() {
        return ResponseEntity.ok(organizationAdminService.listOrganizations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getOrganization(@PathVariable Long id) {
        return ResponseEntity.ok(organizationAdminService.getOrgDetail(id));
    }

    @PostMapping
    public ResponseEntity<OnboardOrgResponse> onboard(@RequestBody OnboardOrgRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizationAdminService.onboard(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Organization> updateStatus(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        return ResponseEntity.ok(organizationAdminService.updateStatus(id, status));
    }
}
