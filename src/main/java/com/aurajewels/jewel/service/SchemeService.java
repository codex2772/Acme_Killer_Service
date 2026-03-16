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
package com.aurajewels.jewel.service;

import com.aurajewels.jewel.entity.Scheme;
import com.aurajewels.jewel.entity.SchemeMember;
import com.aurajewels.jewel.entity.SchemePayment;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.repository.SchemeMemberRepository;
import com.aurajewels.jewel.repository.SchemePaymentRepository;
import com.aurajewels.jewel.repository.SchemeRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Diksha Mohite
 */
@Service
@RequiredArgsConstructor
public class SchemeService {

    private final SchemeRepository schemeRepository;
    private final SchemeMemberRepository schemeMemberRepository;
    private final SchemePaymentRepository schemePaymentRepository;
    private final StoreRepository storeRepository;
    private final ActivityLogService activityLogService;

    /** Get all active schemes for current store. */
    @Transactional(readOnly = true)
    public List<Scheme> findAll() {
        Long storeId = StoreContext.getCurrentStoreId();
        return schemeRepository.findByStore_IdAndActiveTrue(storeId);
    }

    /** Get scheme by id (scoped to current store). */
    @Transactional(readOnly = true)
    public Scheme findById(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return schemeRepository
                .findByIdAndStore_Id(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found"));
    }

    /** Create new scheme for current store. */
    @Transactional
    public Scheme create(Scheme scheme) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));
        scheme.setStore(store);
        scheme.setActive(true);
        Scheme saved = schemeRepository.save(scheme);
        activityLogService.log(
                "Created Scheme", "Scheme: " + saved.getName(), "Schemes", "SCHEME", saved.getId());
        return saved;
    }

    /** Update existing scheme. */
    @Transactional
    public Scheme update(Long id, Scheme updatedScheme) {

        Scheme scheme = findById(id);

        scheme.setName(updatedScheme.getName());
        scheme.setDurationMonths(updatedScheme.getDurationMonths());
        scheme.setMonthlyAmount(updatedScheme.getMonthlyAmount());
        scheme.setStatus(updatedScheme.getStatus());
        scheme.setStartDate(updatedScheme.getStartDate());
        scheme.setEndDate(updatedScheme.getEndDate());
        scheme.setBonusMonth(updatedScheme.getBonusMonth());
        scheme.setDescription(updatedScheme.getDescription());

        Scheme saved = schemeRepository.save(scheme);
        activityLogService.log(
                "Updated Scheme", "Scheme: " + saved.getName(), "Schemes", "SCHEME", saved.getId());
        return saved;
    }

    /** Get members of a scheme. */
    @Transactional(readOnly = true)
    public List<SchemeMember> findMembers(Long schemeId) {
        // Validate scheme belongs to current store
        findById(schemeId);
        return schemeMemberRepository.findByScheme_Id(schemeId);
    }

    /** Add member to scheme. */
    @Transactional
    public SchemeMember addMember(Long schemeId, SchemeMember member) {

        Scheme scheme = findById(schemeId);

        member.setScheme(scheme);

        SchemeMember saved = schemeMemberRepository.save(member);
        activityLogService.log(
                "Added Scheme Member",
                "Member: " + saved.getName() + " to Scheme: " + scheme.getName(),
                "Schemes",
                "SCHEME_MEMBER",
                saved.getId());
        return saved;
    }

    /** Record monthly payment. */
    @Transactional
    public SchemePayment recordPayment(Long memberId, SchemePayment payment) {

        SchemeMember member =
                schemeMemberRepository
                        .findById(memberId)
                        .orElseThrow(() -> new IllegalArgumentException("Scheme member not found"));

        payment.setMember(member);

        SchemePayment saved = schemePaymentRepository.save(payment);
        activityLogService.log(
                "Recorded Scheme Payment",
                "Payment for member: " + member.getName() + " Month: " + saved.getMonthNumber(),
                "Schemes",
                "SCHEME_PAYMENT",
                saved.getId());
        return saved;
    }

    /** Get payment history of a member. */
    @Transactional(readOnly = true)
    public List<SchemePayment> findPayments(Long memberId) {
        return schemePaymentRepository.findByMember_Id(memberId);
    }
}
