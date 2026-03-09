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

import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;

    public List<Customer> findAll() {
        Long storeId = StoreContext.getCurrentStoreId();
        return customerRepository.findByStoreIdAndActiveTrue(storeId);
    }

    public Customer findById(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return customerRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    public Customer findByPhone(String phone) {
        Long storeId = StoreContext.getCurrentStoreId();
        return customerRepository
                .findByPhoneAndStoreId(phone, storeId)
                .orElseThrow(() -> new RuntimeException("Customer not found with phone: " + phone));
    }

    public List<Customer> searchByName(String name) {
        Long storeId = StoreContext.getCurrentStoreId();
        return customerRepository.findByFirstNameContainingIgnoreCaseAndStoreIdAndActiveTrue(
                name, storeId);
    }

    @Transactional
    public Customer create(Customer customer) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new RuntimeException("Store not found"));
        customer.setStore(store);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, Customer updated) {
        Customer existing = findById(id);
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setPhone(updated.getPhone());
        existing.setEmail(updated.getEmail());
        existing.setAddressLine1(updated.getAddressLine1());
        existing.setAddressLine2(updated.getAddressLine2());
        existing.setCity(updated.getCity());
        existing.setState(updated.getState());
        existing.setPincode(updated.getPincode());
        existing.setGstin(updated.getGstin());
        existing.setPan(updated.getPan());
        return customerRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Customer existing = findById(id);
        existing.setActive(false);
        customerRepository.save(existing);
    }
}
