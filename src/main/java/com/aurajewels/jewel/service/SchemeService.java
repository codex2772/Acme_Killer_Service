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
import com.aurajewels.jewel.repository.SchemeMemberRepository;
import com.aurajewels.jewel.repository.SchemePaymentRepository;
import com.aurajewels.jewel.repository.SchemeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author Diksha Mohite
 */
@Service
@RequiredArgsConstructor
public class SchemeService {

    private final SchemeRepository schemeRepository;
    private final SchemeMemberRepository schemeMemberRepository;
    private final SchemePaymentRepository schemePaymentRepository;

    /** Get all active schemes. */
    public List<Scheme> findAll() {
        return schemeRepository.findByActiveTrue();
    }

    /** Get scheme by id. */
    public Scheme findById(Long id) {
        return schemeRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Scheme not found"));
    }

    /** Create new scheme. */
    public Scheme create(Scheme scheme) {
        return schemeRepository.save(scheme);
    }

    /** Update existing scheme. */
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

        return schemeRepository.save(scheme);
    }

    /** Get members of a scheme. */
    public List<SchemeMember> findMembers(Long schemeId) {
        return schemeMemberRepository.findByScheme_Id(schemeId);
    }

    /** Add member to scheme. */
    public SchemeMember addMember(Long schemeId, SchemeMember member) {

        Scheme scheme = findById(schemeId);

        member.setScheme(scheme);

        return schemeMemberRepository.save(member);
    }

    /** Record monthly payment. */
    public SchemePayment recordPayment(Long memberId, SchemePayment payment) {

        SchemeMember member =
                schemeMemberRepository
                        .findById(memberId)
                        .orElseThrow(() -> new RuntimeException("Scheme member not found"));

        payment.setMember(member);

        return schemePaymentRepository.save(payment);
    }

    /** Get payment history of a member. */
    public List<SchemePayment> findPayments(Long memberId) {
        return schemePaymentRepository.findByMember_Id(memberId);
    }
}
