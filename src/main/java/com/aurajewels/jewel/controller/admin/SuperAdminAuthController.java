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

import com.aurajewels.jewel.dto.admin.AdminLoginRequest;
import com.aurajewels.jewel.dto.admin.AdminLoginResponse;
import com.aurajewels.jewel.entity.PlatformAdmin;
import com.aurajewels.jewel.entity.PlatformRole;
import com.aurajewels.jewel.repository.PlatformAdminRepository;
import com.aurajewels.jewel.service.admin.SuperAdminAuthService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class SuperAdminAuthController {

    private final SuperAdminAuthService superAdminAuthService;
    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(superAdminAuthService.login(request));
    }

    /**
     * One-time bootstrap endpoint. Creates the first super admin. Only works when NO platform
     * admins exist in the DB. Returns 409 if admins already exist.
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<?> bootstrap(@RequestBody Map<String, String> request) {
        if (platformAdminRepository.count() > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(
                            Map.of(
                                    "error",
                                    "Bootstrap already completed. Super admin(s) already exist."));
        }

        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");
        String phone = request.getOrDefault("phone", null);

        if (name == null || email == null || password == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "name, email and password are required"));
        }

        PlatformAdmin admin =
                PlatformAdmin.builder()
                        .name(name)
                        .email(email)
                        .phone(phone)
                        .passwordHash(passwordEncoder.encode(password))
                        .role(PlatformRole.SUPER_ADMIN)
                        .active(true)
                        .build();

        platformAdminRepository.save(admin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        Map.of(
                                "message",
                                "Super admin created successfully",
                                "email",
                                email,
                                "name",
                                name));
    }
}
