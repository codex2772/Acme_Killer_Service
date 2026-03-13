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
package com.aurajewels.jewel.service;

import com.aurajewels.jewel.dto.supplier.SupplierRequest;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.entity.Supplier;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.repository.SupplierRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final StoreRepository storeRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<Supplier> listSuppliers() {
        Long storeId = StoreContext.getCurrentStoreId();
        return supplierRepository.findByStoreIdAndActiveTrueOrderByNameAsc(storeId);
    }

    @Transactional(readOnly = true)
    public Supplier getSupplier(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return supplierRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
    }

    @Transactional
    public Supplier createSupplier(SupplierRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Supplier supplier =
                Supplier.builder()
                        .store(store)
                        .name(request.getName())
                        .phone(request.getPhone())
                        .email(request.getEmail())
                        .city(request.getCity())
                        .address(request.getAddress())
                        .gst(request.getGst())
                        .metals(request.getMetals() != null ? request.getMetals() : List.of())
                        .status(
                                request.getStatus() != null
                                        ? Supplier.SupplierStatus.valueOf(request.getStatus())
                                        : Supplier.SupplierStatus.ACTIVE)
                        .active(true)
                        .build();

        supplier = supplierRepository.save(supplier);

        activityLogService.log(
                "Created Supplier",
                "Supplier: " + request.getName(),
                "Accounts",
                "SUPPLIER",
                supplier.getId());

        return supplier;
    }

    @Transactional
    public Supplier updateSupplier(Long id, SupplierRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Supplier supplier =
                supplierRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));

        if (request.getName() != null) supplier.setName(request.getName());
        if (request.getPhone() != null) supplier.setPhone(request.getPhone());
        if (request.getEmail() != null) supplier.setEmail(request.getEmail());
        if (request.getCity() != null) supplier.setCity(request.getCity());
        if (request.getAddress() != null) supplier.setAddress(request.getAddress());
        if (request.getGst() != null) supplier.setGst(request.getGst());
        if (request.getMetals() != null) supplier.setMetals(request.getMetals());
        if (request.getStatus() != null)
            supplier.setStatus(Supplier.SupplierStatus.valueOf(request.getStatus()));

        return supplierRepository.save(supplier);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        Supplier supplier =
                supplierRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Supplier not found"));
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }
}
