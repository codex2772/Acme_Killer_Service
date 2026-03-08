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

import com.aurajewels.jewel.entity.MetalType;
import com.aurajewels.jewel.repository.MetalTypeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MetalTypeService {

    private final MetalTypeRepository metalTypeRepository;

    public List<MetalType> findAll() {
        return metalTypeRepository.findByActiveTrue();
    }

    public MetalType findById(Long id) {
        return metalTypeRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("MetalType not found with id: " + id));
    }

    public List<MetalType> findByName(String name) {
        return metalTypeRepository.findByNameAndActiveTrue(name);
    }

    @Transactional
    public MetalType create(MetalType metalType) {
        return metalTypeRepository.save(metalType);
    }

    @Transactional
    public MetalType update(Long id, MetalType updated) {
        MetalType existing = findById(id);
        existing.setName(updated.getName());
        existing.setPurity(updated.getPurity());
        existing.setCurrentRate(updated.getCurrentRate());
        existing.setUnit(updated.getUnit());
        return metalTypeRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        MetalType existing = findById(id);
        existing.setActive(false);
        metalTypeRepository.save(existing);
    }
}
