/*
 * MIT License
 *
 * Copyright (c) 2026 AuraJewels
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

import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello World controller for testing deployments. This will be replaced with actual business
 * controllers.
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${info.app.version:0.0.1-SNAPSHOT}")
    private String appVersion;

    @GetMapping
    public ResponseEntity<HelloResponse> hello() {
        return ResponseEntity.ok(
                HelloResponse.builder()
                        .message("Welcome to JewelERP!")
                        .service(appName)
                        .version(appVersion)
                        .timestamp(Instant.now())
                        .status("UP")
                        .build());
    }

    @GetMapping("/hello")
    public ResponseEntity<HelloResponse> helloWorld() {
        return ResponseEntity.ok(
                HelloResponse.builder()
                        .message("Hello, World! JewelERP is running.")
                        .service(appName)
                        .version(appVersion)
                        .timestamp(Instant.now())
                        .status("UP")
                        .build());
    }

    @Data
    @Builder
    public static class HelloResponse {
        private String message;
        private String service;
        private String version;
        private Instant timestamp;
        private String status;
    }
}
