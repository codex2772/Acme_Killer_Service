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

import com.aurajewels.jewel.dto.scheme.AddMemberRequest;
import com.aurajewels.jewel.dto.scheme.InstallmentResponse;
import com.aurajewels.jewel.dto.scheme.RecordPaymentRequest;
import com.aurajewels.jewel.dto.scheme.SchemeMemberResponse;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.Scheme;
import com.aurajewels.jewel.entity.SchemeMember;
import com.aurajewels.jewel.entity.SchemePayment;
import com.aurajewels.jewel.entity.Store;
import com.aurajewels.jewel.exception.ConflictException;
import com.aurajewels.jewel.repository.CustomerRepository;
import com.aurajewels.jewel.repository.SchemeMemberRepository;
import com.aurajewels.jewel.repository.SchemePaymentRepository;
import com.aurajewels.jewel.repository.SchemeRepository;
import com.aurajewels.jewel.repository.StoreRepository;
import com.aurajewels.jewel.security.StoreContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Savings scheme service for managing schemes, member enrollment, and installment payments.
 *
 * @author Diksha Mohite
 */
@Service
@RequiredArgsConstructor
public class SchemeService {

    private final SchemeRepository schemeRepository;
    private final SchemeMemberRepository schemeMemberRepository;
    private final SchemePaymentRepository schemePaymentRepository;
    private final StoreRepository storeRepository;
    private final CustomerRepository customerRepository;
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

    /** Get members of a scheme, enriched with derived totalPaid / paid-month counts. */
    @Transactional(readOnly = true)
    public List<SchemeMemberResponse> findMembers(Long schemeId) {
        Scheme scheme = findById(schemeId); // validates scheme belongs to current store
        List<SchemeMember> members = schemeMemberRepository.findByScheme_Id(schemeId);
        List<SchemeMemberResponse> result = new ArrayList<>(members.size());
        for (SchemeMember member : members) {
            List<SchemePayment> payments =
                    schemePaymentRepository.findByMember_IdOrderByMonthNumberAsc(member.getId());
            BigDecimal totalPaid = BigDecimal.ZERO;
            int paidMonths = 0;
            BigDecimal monthly = scheme.getMonthlyAmount();
            for (SchemePayment p : payments) {
                totalPaid = totalPaid.add(p.getAmount());
                if (monthly != null
                        && monthly.signum() > 0
                        && p.getAmount().compareTo(monthly) >= 0) {
                    paidMonths++;
                }
            }
            result.add(
                    SchemeMemberResponse.builder()
                            .id(member.getId())
                            .name(member.getName())
                            .phone(member.getPhone())
                            .joinDate(member.getJoinDate())
                            .status(member.getStatus() != null ? member.getStatus().name() : null)
                            .active(member.getActive())
                            .totalPaid(totalPaid)
                            .paidMonths(paidMonths)
                            .durationMonths(scheme.getDurationMonths())
                            .build());
        }
        return result;
    }

    /**
     * Add a member to a scheme, enforcing that the same customer cannot be enrolled twice. When
     * {@code customerId} is supplied the member is linked to that customer and a duplicate is
     * rejected with a 409; manual-entry members are de-duplicated within the scheme on normalized
     * phone.
     */
    @Transactional
    public SchemeMember addMember(Long schemeId, AddMemberRequest request) {
        Scheme scheme = findById(schemeId); // validates scheme belongs to current store
        Long storeId = StoreContext.getCurrentStoreId();

        SchemeMember.SchemeMemberBuilder builder = SchemeMember.builder().scheme(scheme);
        String name = request.getName();
        String phone = request.getPhone();

        if (request.getCustomerId() != null) {
            if (schemeMemberRepository.existsByScheme_IdAndCustomer_Id(
                    schemeId, request.getCustomerId())) {
                throw new ConflictException("Customer is already a member of this scheme");
            }
            Customer customer =
                    customerRepository
                            .findByIdAndStoreId(request.getCustomerId(), storeId)
                            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
            builder.customer(customer);
            if (name == null || name.isBlank()) {
                name =
                        customer.getFirstName()
                                + (customer.getLastName() != null
                                        ? " " + customer.getLastName()
                                        : "");
            }
            if (phone == null || phone.isBlank()) {
                phone = customer.getPhone();
            }
        } else {
            // Manual-entry member: de-duplicate within the scheme on normalized phone.
            String normalized = normalizePhone(phone);
            if (normalized.length() >= 10) {
                boolean duplicate =
                        schemeMemberRepository.findByScheme_Id(schemeId).stream()
                                .anyMatch(m -> normalized.equals(normalizePhone(m.getPhone())));
                if (duplicate) {
                    throw new ConflictException(
                            "A member with this phone is already in this scheme");
                }
            }
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Member name is required");
        }

        SchemeMember member =
                builder.name(name)
                        .phone(phone)
                        .joinDate(
                                request.getJoinDate() != null
                                        ? request.getJoinDate()
                                        : LocalDate.now())
                        .status(
                                request.getStatus() != null
                                        ? request.getStatus()
                                        : SchemeMember.MemberStatus.ACTIVE)
                        .build();
        member.setActive(true);

        SchemeMember saved = schemeMemberRepository.save(member);
        activityLogService.log(
                "Added Scheme Member",
                "Member: " + saved.getName() + " to Scheme: " + scheme.getName(),
                "Schemes",
                "SCHEME_MEMBER",
                saved.getId());
        return saved;
    }

    /** Normalize a phone to its trailing digits for de-duplication (empty when null/blank). */
    private static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    /**
     * Record an installment payment for a member and return the recomputed schedule.
     *
     * <p>One row is kept per (member, month); a payment toward an already-started month tops up the
     * existing row (basic partial-payment support) and flips it to PAID once the cumulative amount
     * reaches the scheme's monthly amount. Fully paid months are rejected to prevent overpayment.
     */
    @Transactional
    public List<InstallmentResponse> recordPayment(
            Long schemeId, Long memberId, RecordPaymentRequest request) {
        Scheme scheme = findById(schemeId); // validates scheme belongs to current store
        SchemeMember member = loadMember(schemeId, memberId);

        if (request.getMonth() == null
                || request.getMonth() < 1
                || request.getMonth() > scheme.getDurationMonths()) {
            throw new IllegalArgumentException(
                    "Invalid installment month: " + request.getMonth());
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        BigDecimal monthly = scheme.getMonthlyAmount();
        SchemePayment row =
                schemePaymentRepository
                        .findByMember_IdAndMonthNumber(memberId, request.getMonth())
                        .orElse(null);

        if (row != null && monthly != null && row.getAmount().compareTo(monthly) >= 0) {
            throw new IllegalArgumentException(
                    "Month " + request.getMonth() + " is already fully paid");
        }

        BigDecimal cumulative =
                (row != null ? row.getAmount() : BigDecimal.ZERO).add(request.getAmount());
        LocalDate paymentDate = request.getDate() != null ? request.getDate() : LocalDate.now();
        boolean fullyPaid = monthly != null && cumulative.compareTo(monthly) >= 0;

        if (row == null) {
            row =
                    SchemePayment.builder()
                            .member(member)
                            .monthNumber(request.getMonth())
                            .build();
        }
        row.setAmount(cumulative);
        row.setPaymentDate(paymentDate);
        row.setStatus(
                fullyPaid ? SchemePayment.PaymentStatus.PAID : SchemePayment.PaymentStatus.PENDING);
        schemePaymentRepository.save(row);

        activityLogService.log(
                "Recorded Scheme Payment",
                "Payment for member: " + member.getName() + " Month: " + request.getMonth(),
                "Schemes",
                "SCHEME_PAYMENT",
                row.getId());

        return buildSchedule(scheme, member);
    }

    /** Get the installment schedule for a member (PAID / DUE / UPCOMING per month). */
    @Transactional(readOnly = true)
    public List<InstallmentResponse> findPayments(Long schemeId, Long memberId) {
        Scheme scheme = findById(schemeId); // validates scheme belongs to current store
        SchemeMember member = loadMember(schemeId, memberId);
        return buildSchedule(scheme, member);
    }

    private SchemeMember loadMember(Long schemeId, Long memberId) {
        return schemeMemberRepository
                .findByIdAndScheme_Id(memberId, schemeId)
                .orElseThrow(() -> new IllegalArgumentException("Scheme member not found"));
    }

    /**
     * Derive the full installment schedule from the scheme term and the member's recorded payments.
     *
     * <p>A month is PAID once its cumulative recorded amount reaches the monthly amount. The first
     * not-yet-fully-paid month whose due date has arrived is DUE; every later month is UPCOMING.
     * This yields exactly one DUE installment at a time and advances automatically as payments land,
     * so no schedule rows need to be persisted up front.
     */
    private List<InstallmentResponse> buildSchedule(Scheme scheme, SchemeMember member) {
        int months = scheme.getDurationMonths() != null ? scheme.getDurationMonths() : 0;
        BigDecimal monthly =
                scheme.getMonthlyAmount() != null ? scheme.getMonthlyAmount() : BigDecimal.ZERO;
        LocalDate start = scheme.getStartDate();
        LocalDate today = LocalDate.now();
        // Once the scheme has started, the next unpaid installment is the collection target (DUE);
        // before it starts, nothing is due yet.
        boolean schemeStarted = start == null || !start.isAfter(today);

        Map<Integer, SchemePayment> paidByMonth = new HashMap<>();
        for (SchemePayment p : schemePaymentRepository.findByMember_IdOrderByMonthNumberAsc(
                member.getId())) {
            paidByMonth.put(p.getMonthNumber(), p);
        }

        // First month not yet fully paid — the sequential collection target.
        int firstUnpaid = Integer.MAX_VALUE;
        for (int i = 1; i <= months; i++) {
            SchemePayment p = paidByMonth.get(i);
            boolean fullyPaid = p != null && monthly.signum() > 0 && p.getAmount().compareTo(monthly) >= 0;
            if (!fullyPaid) {
                firstUnpaid = i;
                break;
            }
        }

        List<InstallmentResponse> schedule = new ArrayList<>(months);
        for (int i = 1; i <= months; i++) {
            LocalDate dueDate = start != null ? start.plusMonths(i - 1L) : today;
            SchemePayment p = paidByMonth.get(i);
            boolean fullyPaid =
                    p != null && monthly.signum() > 0 && p.getAmount().compareTo(monthly) >= 0;

            String status;
            BigDecimal amount;
            LocalDate date;
            if (fullyPaid) {
                status = "PAID";
                amount = p.getAmount();
                date = p.getPaymentDate();
            } else if (i == firstUnpaid && schemeStarted) {
                status = "DUE";
                amount = monthly;
                date = dueDate;
            } else {
                status = "UPCOMING";
                amount = monthly;
                date = dueDate;
            }
            schedule.add(
                    InstallmentResponse.builder()
                            .month(i)
                            .amount(amount)
                            .date(date)
                            .status(status)
                            .build());
        }
        return schedule;
    }
}
