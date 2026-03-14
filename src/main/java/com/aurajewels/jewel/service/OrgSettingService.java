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
package com.aurajewels.jewel.service;

import com.aurajewels.jewel.entity.OrgSetting;
import com.aurajewels.jewel.entity.Organization;
import com.aurajewels.jewel.repository.OrgSettingRepository;
import com.aurajewels.jewel.repository.OrganizationRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class OrgSettingService {

    private final OrgSettingRepository orgSettingRepository;
    private final OrganizationRepository organizationRepository;

    @Transactional(readOnly = true)
    public Map<String, String> getSettings() {
        Long orgId = StoreContext.getCurrentOrgId();
        List<OrgSetting> settings = orgSettingRepository.findByOrganizationId(orgId);
        Map<String, String> result = new HashMap<>();
        for (OrgSetting s : settings) {
            result.put(s.getSettingKey(), s.getSettingValue());
        }
        return result;
    }

    @Transactional
    public Map<String, String> updateSettings(Map<String, String> settings) {
        Long orgId = StoreContext.getCurrentOrgId();
        Organization org =
                organizationRepository
                        .findById(orgId)
                        .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

        for (Map.Entry<String, String> entry : settings.entrySet()) {
            OrgSetting setting =
                    orgSettingRepository
                            .findByOrganizationIdAndSettingKey(orgId, entry.getKey())
                            .orElseGet(
                                    () ->
                                            OrgSetting.builder()
                                                    .organization(org)
                                                    .settingKey(entry.getKey())
                                                    .build());
            setting.setSettingValue(entry.getValue());
            orgSettingRepository.save(setting);
        }

        return getSettings();
    }

    @Transactional(readOnly = true)
    public List<String> getExpenseCategories() {
        return List.of(
                "Salary",
                "Rent",
                "Utilities",
                "Insurance",
                "Maintenance",
                "Marketing",
                "Repairs",
                "Travel",
                "Office Supplies",
                "Miscellaneous");
    }
}
