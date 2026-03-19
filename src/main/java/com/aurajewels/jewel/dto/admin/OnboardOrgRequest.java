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
package com.aurajewels.jewel.dto.admin;

import java.util.List;
import lombok.Data;

/**
 * Request body for onboarding a new organization with stores and owner.
 *
 * @author Raviraj Bhosale
 */
@Data
public class OnboardOrgRequest {

    private OrgInfo organization;
    private OwnerInfo owner;
    private List<StoreInfo> stores;
    private SubscriptionInfo subscription;

    @Data
    public static class OrgInfo {
        private String name;
    }

    @Data
    public static class OwnerInfo {
        private String name;
        private String mobile;
        private String email;
        private String password;
    }

    @Data
    public static class StoreInfo {
        private String name;
        private String address;
        private String city;
        private String phone;
        private String gstin;
        private List<String> modules; // module codes to enable for this store
    }

    @Data
    public static class SubscriptionInfo {
        private String plan; // STARTER, PROFESSIONAL, ENTERPRISE, CUSTOM
        private Integer trialDays;
        private Integer maxStores;
        private Integer maxUsers;
    }
}
