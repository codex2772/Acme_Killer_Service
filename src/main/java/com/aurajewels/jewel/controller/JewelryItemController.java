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

import com.aurajewels.jewel.entity.JewelryItem;
import com.aurajewels.jewel.security.RequiresPermission;
import com.aurajewels.jewel.service.JewelryItemService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jewelry-items")
@RequiredArgsConstructor
public class JewelryItemController {

    private final JewelryItemService jewelryItemService;

    @GetMapping
    @RequiresPermission("VIEW_INVENTORY")
    public ResponseEntity<List<JewelryItem>> getAll() {
        return ResponseEntity.ok(jewelryItemService.findAll());
    }

    @GetMapping("/{id}")
    @RequiresPermission("VIEW_INVENTORY")
    public ResponseEntity<JewelryItem> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jewelryItemService.findById(id));
    }

    @GetMapping("/sku/{sku}")
    @RequiresPermission("VIEW_INVENTORY")
    public ResponseEntity<JewelryItem> getBySku(@PathVariable String sku) {
        return ResponseEntity.ok(jewelryItemService.findBySku(sku));
    }

    @GetMapping("/category/{categoryId}")
    @RequiresPermission("VIEW_INVENTORY")
    public ResponseEntity<List<JewelryItem>> getByCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok(jewelryItemService.findByCategory(categoryId));
    }

    @GetMapping("/status/{status}")
    @RequiresPermission("VIEW_INVENTORY")
    public ResponseEntity<List<JewelryItem>> getByStatus(
            @PathVariable JewelryItem.ItemStatus status) {
        return ResponseEntity.ok(jewelryItemService.findByStatus(status));
    }

    @PostMapping
    @RequiresPermission("MANAGE_INVENTORY")
    public ResponseEntity<JewelryItem> create(@RequestBody JewelryItem item) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jewelryItemService.create(item));
    }

    @PutMapping("/{id}")
    @RequiresPermission("MANAGE_INVENTORY")
    public ResponseEntity<JewelryItem> update(
            @PathVariable Long id, @RequestBody JewelryItem item) {
        return ResponseEntity.ok(jewelryItemService.update(id, item));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("MANAGE_INVENTORY")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jewelryItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
