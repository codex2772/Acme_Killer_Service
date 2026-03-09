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

import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.security.RequiresPermission;
import com.aurajewels.jewel.service.CustomerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @RequiresPermission("VIEW_CUSTOMERS")
    public ResponseEntity<List<Customer>> getAll() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/{id}")
    @RequiresPermission("VIEW_CUSTOMERS")
    public ResponseEntity<Customer> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    @GetMapping("/phone/{phone}")
    @RequiresPermission("VIEW_CUSTOMERS")
    public ResponseEntity<Customer> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(customerService.findByPhone(phone));
    }

    @GetMapping("/search")
    @RequiresPermission("VIEW_CUSTOMERS")
    public ResponseEntity<List<Customer>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(customerService.searchByName(name));
    }

    @PostMapping
    @RequiresPermission("MANAGE_CUSTOMERS")
    public ResponseEntity<Customer> create(@RequestBody Customer customer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(customer));
    }

    @PutMapping("/{id}")
    @RequiresPermission("MANAGE_CUSTOMERS")
    public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.update(id, customer));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("MANAGE_CUSTOMERS")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
