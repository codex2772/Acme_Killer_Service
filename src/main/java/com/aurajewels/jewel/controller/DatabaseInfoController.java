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

import com.aurajewels.jewel.security.RequiresPermission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Raviraj Bhosale
 */
@RestController
@RequestMapping("/api/db")
@RequiredArgsConstructor
public class DatabaseInfoController {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    @GetMapping("/status")
    @RequiresPermission("VIEW_REPORTS")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> info = new HashMap<>();
        try {
            String dbVersion = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
            String dbName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            Long categoryCount =
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM categories", Long.class);
            Long metalTypeCount =
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM metal_types", Long.class);
            Long customerCount =
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM customers", Long.class);
            Long jewelryItemCount =
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM jewelry_items", Long.class);
            Long invoiceCount =
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM invoices", Long.class);
            Long orgCount =
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organizations", Long.class);
            Long storeCount =
                    jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores", Long.class);
            Long userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);

            info.put("connected", true);
            info.put("database", dbName);
            info.put("version", dbVersion);
            info.put(
                    "tables",
                    Map.of(
                            "organizations", orgCount,
                            "stores", storeCount,
                            "users", userCount,
                            "categories", categoryCount,
                            "metal_types", metalTypeCount,
                            "customers", customerCount,
                            "jewelry_items", jewelryItemCount,
                            "invoices", invoiceCount));
        } catch (Exception e) {
            info.put("connected", false);
            info.put("error", e.getMessage());
        }
        return ResponseEntity.ok(info);
    }

    @GetMapping("/users")
    @RequiresPermission("MANAGE_STAFF")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> users =
                jdbcTemplate.queryForList(
                        "SELECT u.id, u.name, u.mobile, u.role, u.active, o.name AS org_name "
                                + "FROM users u JOIN organizations o ON u.org_id = o.id "
                                + "ORDER BY u.id");
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/access")
    @RequiresPermission("MANAGE_STAFF")
    public ResponseEntity<List<Map<String, Object>>> listUserAccess() {
        List<Map<String, Object>> access =
                jdbcTemplate.queryForList(
                        "SELECT u.name AS user_name, u.role, s.name AS store_name "
                                + "FROM user_store_access usa "
                                + "JOIN users u ON usa.user_id = u.id "
                                + "JOIN stores s ON usa.store_id = s.id "
                                + "ORDER BY u.id, s.id");
        return ResponseEntity.ok(access);
    }

    @GetMapping("/users/permissions")
    @RequiresPermission("MANAGE_STAFF")
    public ResponseEntity<List<Map<String, Object>>> listUserPermissions() {
        List<Map<String, Object>> perms =
                jdbcTemplate.queryForList(
                        "SELECT u.name AS user_name, u.role, s.name AS store_name, p.name AS permission "
                                + "FROM user_permissions up "
                                + "JOIN users u ON up.user_id = u.id "
                                + "JOIN stores s ON up.store_id = s.id "
                                + "JOIN permissions p ON up.permission_id = p.id "
                                + "ORDER BY u.id, s.id, p.id");
        return ResponseEntity.ok(perms);
    }

    @GetMapping("/stores")
    @RequiresPermission("VIEW_REPORTS")
    public ResponseEntity<List<Map<String, Object>>> listStores() {
        List<Map<String, Object>> stores =
                jdbcTemplate.queryForList(
                        "SELECT s.id, s.name, s.city, s.phone, o.name AS org_name "
                                + "FROM stores s JOIN organizations o ON s.org_id = o.id "
                                + "ORDER BY s.id");
        return ResponseEntity.ok(stores);
    }
}
