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

import com.aurajewels.jewel.dto.scheme.SchemeMemberRequest;
import com.aurajewels.jewel.dto.scheme.SchemePaymentRequest;
import com.aurajewels.jewel.dto.scheme.SchemeRequest;
import com.aurajewels.jewel.entity.*;
import com.aurajewels.jewel.repository.*;
import com.aurajewels.jewel.security.StoreContext;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Raviraj Bhosale
 */
@Service
@RequiredArgsConstructor
public class SchemeService {

    private final SchemeRepository schemeRepository;
    private final SchemeMemberRepository schemeMemberRepository;
    private final SchemePaymentRepository schemePaymentRepository;
    private final CustomerRepository customerRepository;
    private final StoreRepository storeRepository;
    private final ActivityLogService activityLogService;

    @Transactional(readOnly = true)
    public List<Scheme> listSchemes() {
        Long storeId = StoreContext.getCurrentStoreId();
        return schemeRepository.findByStoreIdAndActiveTrueOrderByCreatedAtDesc(storeId);
    }

    @Transactional(readOnly = true)
    public Scheme getScheme(Long id) {
        Long storeId = StoreContext.getCurrentStoreId();
        return schemeRepository
                .findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme not found"));
    }

    @Transactional
    public Scheme createScheme(SchemeRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Store store =
                storeRepository
                        .findById(storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Scheme scheme =
                Scheme.builder()
                        .store(store)
                        .name(request.getName())
                        .durationMonths(request.getDurationMonths())
                        .monthlyAmount(request.getMonthlyAmount())
                        .status(
                                request.getStatus() != null
                                        ? Scheme.SchemeStatus.valueOf(request.getStatus())
                                        : Scheme.SchemeStatus.ACTIVE)
                        .startDate(request.getStartDate())
                        .endDate(request.getEndDate())
                        .bonusMonth(
                                request.getBonusMonth() != null ? request.getBonusMonth() : false)
                        .description(request.getDescription())
                        .active(true)
                        .build();

        scheme = schemeRepository.save(scheme);

        activityLogService.log(
                "Created Scheme",
                "Scheme: " + request.getName() + " — " + request.getDurationMonths() + " months",
                "Schemes",
                "SCHEME",
                scheme.getId());

        return scheme;
    }

    @Transactional
    public Scheme updateScheme(Long id, SchemeRequest request) {
        Long storeId = StoreContext.getCurrentStoreId();
        Scheme scheme =
                schemeRepository
                        .findByIdAndStoreId(id, storeId)
                        .orElseThrow(() -> new IllegalArgumentException("Scheme not found"));

        if (request.getName() != null) scheme.setName(request.getName());
        if (request.getDurationMonths() != null)
            scheme.setDurationMonths(request.getDurationMonths());
        if (request.getMonthlyAmount() != null) scheme.setMonthlyAmount(request.getMonthlyAmount());
        if (request.getStatus() != null)
            scheme.setStatus(Scheme.SchemeStatus.valueOf(request.getStatus()));
        if (request.getStartDate() != null) scheme.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) scheme.setEndDate(request.getEndDate());
        if (request.getBonusMonth() != null) scheme.setBonusMonth(request.getBonusMonth());
        if (request.getDescription() != null) scheme.setDescription(request.getDescription());

        return schemeRepository.save(scheme);
    }

    // --- Members ---

    @Transactional(readOnly = true)
    public List<SchemeMember> listMembers(Long schemeId) {
        // Verify scheme belongs to current store
        getScheme(schemeId);
        return schemeMemberRepository.findBySchemeId(schemeId);
    }

    @Transactional
    public SchemeMember addMember(Long schemeId, SchemeMemberRequest request) {
        Scheme scheme = getScheme(schemeId);

        SchemeMember member =
                SchemeMember.builder()
                        .scheme(scheme)
                        .name(request.getName())
                        .phone(request.getPhone())
                        .joinDate(
                                request.getJoinDate() != null
                                        ? request.getJoinDate()
                                        : LocalDate.now())
                        .status(SchemeMember.MemberStatus.ACTIVE)
                        .build();

        if (request.getCustomerId() != null) {
            Customer customer =
                    customerRepository
                            .findById(request.getCustomerId())
                            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            member.setCustomer(customer);
        }

        member = schemeMemberRepository.save(member);

        activityLogService.log(
                "Added Scheme Member",
                request.getName() + " joined scheme " + scheme.getName(),
                "Schemes",
                "SCHEME_MEMBER",
                member.getId());

        return member;
    }

    // --- Payments ---

    @Transactional
    public SchemePayment recordPayment(Long schemeId, Long memberId, SchemePaymentRequest request) {
        // Verify scheme belongs to current store
        getScheme(schemeId);

        SchemeMember member =
                schemeMemberRepository
                        .findByIdAndSchemeId(memberId, schemeId)
                        .orElseThrow(() -> new IllegalArgumentException("Scheme member not found"));

        SchemePayment payment =
                SchemePayment.builder()
                        .schemeMember(member)
                        .monthNumber(request.getMonth())
                        .amount(request.getAmount())
                        .paymentDate(
                                request.getDate() != null ? request.getDate() : LocalDate.now())
                        .status(SchemePayment.PaymentStatus.PAID)
                        .build();

        payment = schemePaymentRepository.save(payment);

        activityLogService.log(
                "Recorded Scheme Payment",
                member.getName() + " — Month " + request.getMonth() + " — ₹" + request.getAmount(),
                "Schemes",
                "SCHEME_PAYMENT",
                payment.getId());

        return payment;
    }

    @Transactional(readOnly = true)
    public List<SchemePayment> getPaymentHistory(Long schemeId, Long memberId) {
        // Verify scheme belongs to current store
        getScheme(schemeId);
        schemeMemberRepository
                .findByIdAndSchemeId(memberId, schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme member not found"));
        return schemePaymentRepository.findBySchemeMemberIdOrderByMonthNumberAsc(memberId);
    }
}
