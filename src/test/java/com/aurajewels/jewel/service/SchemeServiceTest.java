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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.aurajewels.jewel.dto.scheme.AddMemberRequest;
import com.aurajewels.jewel.dto.scheme.InstallmentResponse;
import com.aurajewels.jewel.dto.scheme.RecordPaymentRequest;
import com.aurajewels.jewel.dto.scheme.SchemeMemberResponse;
import com.aurajewels.jewel.entity.Customer;
import com.aurajewels.jewel.entity.Scheme;
import com.aurajewels.jewel.entity.SchemeMember;
import com.aurajewels.jewel.entity.SchemePayment;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Verifies scheme installment schedule derivation and payment recording. */
class SchemeServiceTest {

    private static final Long STORE_ID = 3L;
    private static final Long SCHEME_ID = 1L;
    private static final Long MEMBER_ID = 10L;

    private SchemePaymentRepository paymentRepository;
    private SchemeMemberRepository memberRepository;
    private CustomerRepository customerRepository;
    private SchemeService service;
    private Scheme scheme;
    private SchemeMember member;

    /** In-memory backing store so the mocked payment repo behaves like a real one. */
    private final List<SchemePayment> payments = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SchemeRepository schemeRepository = Mockito.mock(SchemeRepository.class);
        memberRepository = Mockito.mock(SchemeMemberRepository.class);
        paymentRepository = Mockito.mock(SchemePaymentRepository.class);
        StoreRepository storeRepository = Mockito.mock(StoreRepository.class);
        customerRepository = Mockito.mock(CustomerRepository.class);
        ActivityLogService activityLogService = Mockito.mock(ActivityLogService.class);

        service =
                new SchemeService(
                        schemeRepository,
                        memberRepository,
                        paymentRepository,
                        storeRepository,
                        customerRepository,
                        activityLogService);

        // Gold Monthly Plus: ₹1,000 × 11 months, starting today so month 1 is the current period.
        scheme =
                Scheme.builder()
                        .name("Gold Monthly Plus")
                        .durationMonths(11)
                        .monthlyAmount(new BigDecimal("1000.00"))
                        .startDate(LocalDate.now())
                        .build();
        scheme.setId(SCHEME_ID);

        member = SchemeMember.builder().name("Shubham Mane").scheme(scheme).build();
        member.setId(MEMBER_ID);
        member.setStatus(SchemeMember.MemberStatus.ACTIVE);

        StoreContext ctx = new StoreContext();
        ctx.setStoreId(STORE_ID);
        StoreContext.set(ctx);

        when(schemeRepository.findByIdAndStore_Id(SCHEME_ID, STORE_ID))
                .thenReturn(Optional.of(scheme));
        when(memberRepository.findByIdAndScheme_Id(MEMBER_ID, SCHEME_ID))
                .thenReturn(Optional.of(member));
        when(memberRepository.findByScheme_Id(SCHEME_ID)).thenReturn(List.of(member));

        // Stateful payment repo backed by `payments`.
        when(paymentRepository.findByMember_IdOrderByMonthNumberAsc(anyLong()))
                .thenAnswer(
                        inv ->
                                payments.stream()
                                        .sorted(Comparator.comparingInt(SchemePayment::getMonthNumber))
                                        .toList());
        when(paymentRepository.findByMember_IdAndMonthNumber(anyLong(), anyInt()))
                .thenAnswer(
                        inv -> {
                            Integer m = inv.getArgument(1);
                            return payments.stream()
                                    .filter(p -> p.getMonthNumber().equals(m))
                                    .findFirst();
                        });
        when(paymentRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            SchemePayment p = inv.getArgument(0);
                            if (p.getId() == null) {
                                p.setId((long) (payments.size() + 1));
                                payments.add(p);
                            }
                            return p;
                        });
    }

    @AfterEach
    void tearDown() {
        StoreContext.clear();
    }

    @Test
    void freshMemberScheduleHasExactlyCurrentMonthDue() {
        List<InstallmentResponse> schedule = service.findPayments(SCHEME_ID, MEMBER_ID);

        assertThat(schedule).hasSize(11);
        assertThat(schedule).filteredOn(i -> "DUE".equals(i.getStatus())).hasSize(1);
        assertThat(schedule.get(0).getStatus()).isEqualTo("DUE");
        assertThat(schedule.get(0).getMonth()).isEqualTo(1);
        assertThat(schedule.get(0).getAmount()).isEqualByComparingTo("1000.00");
        assertThat(schedule).allSatisfy(i -> assertThat(i.getStatus()).isNotEqualTo("PAID"));
        assertThat(schedule.subList(1, 11))
                .allSatisfy(i -> assertThat(i.getStatus()).isEqualTo("UPCOMING"));
    }

    @Test
    void recordingPaymentMarksMonthPaidAndAdvancesDue() {
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setMonth(1);
        req.setAmount(new BigDecimal("1000.00"));
        req.setDate(LocalDate.now());

        List<InstallmentResponse> schedule = service.recordPayment(SCHEME_ID, MEMBER_ID, req);

        assertThat(schedule.get(0).getStatus()).isEqualTo("PAID");
        assertThat(schedule.get(0).getAmount()).isEqualByComparingTo("1000.00");
        assertThat(schedule.get(1).getStatus()).isEqualTo("DUE");
        assertThat(schedule.get(1).getMonth()).isEqualTo(2);
        assertThat(schedule).filteredOn(i -> "DUE".equals(i.getStatus())).hasSize(1);
    }

    @Test
    void partialPaymentKeepsMonthDueUntilFullyPaid() {
        RecordPaymentRequest first = new RecordPaymentRequest();
        first.setMonth(1);
        first.setAmount(new BigDecimal("400.00"));
        service.recordPayment(SCHEME_ID, MEMBER_ID, first);

        List<InstallmentResponse> afterPartial = service.findPayments(SCHEME_ID, MEMBER_ID);
        assertThat(afterPartial.get(0).getStatus()).isEqualTo("DUE");

        // totalPaid reflects the partial amount even though the month is not yet PAID.
        SchemeMemberResponse m = service.findMembers(SCHEME_ID).get(0);
        assertThat(m.getTotalPaid()).isEqualByComparingTo("400.00");
        assertThat(m.getPaidMonths()).isZero();

        RecordPaymentRequest topUp = new RecordPaymentRequest();
        topUp.setMonth(1);
        topUp.setAmount(new BigDecimal("600.00"));
        List<InstallmentResponse> afterFull = service.recordPayment(SCHEME_ID, MEMBER_ID, topUp);

        assertThat(afterFull.get(0).getStatus()).isEqualTo("PAID");
        assertThat(afterFull.get(1).getStatus()).isEqualTo("DUE");
        assertThat(service.findMembers(SCHEME_ID).get(0).getPaidMonths()).isEqualTo(1);
    }

    @Test
    void payingAFullyPaidMonthIsRejected() {
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setMonth(1);
        req.setAmount(new BigDecimal("1000.00"));
        service.recordPayment(SCHEME_ID, MEMBER_ID, req);

        assertThatThrownBy(() -> service.recordPayment(SCHEME_ID, MEMBER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already fully paid");
    }

    @Test
    void invalidMonthIsRejected() {
        RecordPaymentRequest req = new RecordPaymentRequest();
        req.setMonth(12); // scheme only has 11 months
        req.setAmount(new BigDecimal("1000.00"));

        assertThatThrownBy(() -> service.recordPayment(SCHEME_ID, MEMBER_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid installment month");
    }

    @Test
    void enrollingAnAlreadyEnrolledCustomerIsRejected() {
        when(memberRepository.existsByScheme_IdAndCustomer_Id(SCHEME_ID, 55L)).thenReturn(true);

        AddMemberRequest req = new AddMemberRequest();
        req.setCustomerId(55L);
        req.setName("Rushi");

        assertThatThrownBy(() -> service.addMember(SCHEME_ID, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a member of this scheme");
    }

    @Test
    void enrollingANewCustomerSucceeds() {
        when(memberRepository.existsByScheme_IdAndCustomer_Id(SCHEME_ID, 77L)).thenReturn(false);
        Customer customer = new Customer();
        customer.setId(77L);
        customer.setFirstName("Rushi");
        customer.setPhone("9998887777");
        when(customerRepository.findByIdAndStoreId(77L, STORE_ID))
                .thenReturn(java.util.Optional.of(customer));
        when(memberRepository.save(any()))
                .thenAnswer(
                        inv -> {
                            SchemeMember m = inv.getArgument(0);
                            m.setId(99L);
                            return m;
                        });

        AddMemberRequest req = new AddMemberRequest();
        req.setCustomerId(77L);

        SchemeMember saved = service.addMember(SCHEME_ID, req);
        assertThat(saved.getCustomer()).isSameAs(customer);
        assertThat(saved.getName()).isEqualTo("Rushi");
        assertThat(saved.getStatus()).isEqualTo(SchemeMember.MemberStatus.ACTIVE);
    }

    @Test
    void manualMemberWithDuplicatePhoneIsRejected() {
        SchemeMember existing = SchemeMember.builder().name("Walk-in").phone("+91 99988-87777").build();
        when(memberRepository.findByScheme_Id(SCHEME_ID)).thenReturn(List.of(existing));

        AddMemberRequest req = new AddMemberRequest();
        req.setName("Same Person");
        req.setPhone("9998887777"); // same 10 digits, different formatting

        assertThatThrownBy(() -> service.addMember(SCHEME_ID, req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("phone is already in this scheme");
    }
}
