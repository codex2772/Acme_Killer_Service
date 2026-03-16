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
package com.aurajewels.jewel.dto.customerapp;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for store detail view in the customer mobile app. Includes store info, categories,
 * and catalog summary.
 *
 * @author Raviraj Bhosale
 */
@Data
@Builder
public class StoreDetailResponse {
    private Long id;
    private String name;
    private String organizationName;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String phone;
    private String gstin;
    private List<CategoryInfo> categories;
    private int totalItems;

    @Data
    @Builder
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String description;
        private long itemCount;
    }
}
