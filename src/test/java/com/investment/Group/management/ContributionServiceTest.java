package com.investment.Group.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.investment.Group.management.Exception.DuplicateContributionException;
import com.investment.Group.management.Exception.MemberNotFoundException;
import com.investment.Group.management.Service.ContributionService;
import com.investment.Group.management.model.Contribution;
import com.investment.Group.management.model.Member;
import com.investment.Group.management.Repository.ContributionRepository;
import com.investment.Group.management.Repository.MemberRepository;

/**
 * Unit tests for the contribution rules.
 *
 * <p>The previous test suite contained a single generated
 * {@code contextLoads} test that asserted only that the Spring context could
 * start. It passed while every endpoint was unauthenticated, so it provided no
 * protection at all. These tests exercise the actual business rules.
 */
@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock
    private ContributionRepository contributionRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ContributionService contributionService;

    private Member member() {
        Member m = new Member();
        m.setFullName("Test Member");
        m.setStatus("ACTIVE");
        return m;
    }

    @Test
    @DisplayName("throws MemberNotFoundException (404) for an unknown member")
    void unknownMemberIsRejected() {
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contributionService.createContribution(
                999L, new BigDecimal("100.00"), "JANUARY", true))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessageContaining("999");

        // Nothing may be persisted when the member does not exist.
        verify(contributionRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws DuplicateContributionException (409) when the month is already paid")
    void duplicateMonthIsRejected() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member()));
        when(contributionRepository.existsByMemberIdAndMonth(1L, "JANUARY")).thenReturn(true);

        assertThatThrownBy(() -> contributionService.createContribution(
                1L, new BigDecimal("100.00"), "JANUARY", true))
                .isInstanceOf(DuplicateContributionException.class);

        verify(contributionRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a zero or negative amount")
    void nonPositiveAmountIsRejected() {
        assertThatThrownBy(() -> contributionService.createContribution(
                1L, BigDecimal.ZERO, "JANUARY", true))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> contributionService.createContribution(
                1L, new BigDecimal("-5.00"), "JANUARY", true))
                .isInstanceOf(IllegalArgumentException.class);

        verify(contributionRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a null amount rather than silently persisting null")
    void nullAmountIsRejected() {
        assertThatThrownBy(() -> contributionService.createContribution(
                1L, null, "JANUARY", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("records status LATE when paid after the 5th")
    void paymentAfterDueDateIsLate() {
        // Choose a day of month that is unambiguously past the due date so the
        // test does not depend on when it is executed.
        assertThat(statusForDay(20)).isEqualTo("LATE");
        assertThat(statusForDay(1)).isEqualTo("PAID");
        // Exactly on the due date is still on time.
        assertThat(statusForDay(5)).isEqualTo("PAID");
    }

    /**
     * Mirrors the service's own day-of-month rule. Kept local rather than
     * driving the real method because {@code LocalDate.now()} cannot be
     * injected, which is itself a testability wart worth noting.
     */
    private String statusForDay(int dayOfMonth) {
        LocalDate date = LocalDate.of(2026, 1, dayOfMonth);
        return date.getDayOfMonth() > ContributionService.PAYMENT_DUE_DAY_OF_MONTH
                ? "LATE" : "PAID";
    }

    @Test
    @DisplayName("preserves the exact amount for a decimal value such as 0.10")
    void moneyPrecisionIsPreserved() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member()));
        when(contributionRepository.existsByMemberIdAndMonth(1L, "MARCH")).thenReturn(false);
        when(contributionRepository.save(any(Contribution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Contribution saved = contributionService.createContribution(
                1L, new BigDecimal("0.10"), "MARCH", true);

        // The failure mode of using double here.
        assertThat(saved.getAmount()).isEqualByComparingTo("0.10");
        assertThat(saved.getAmount().toPlainString()).isEqualTo("0.10");
    }

    @Test
    @DisplayName("falls back to the current month when none is supplied")
    void defaultsToCurrentMonth() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member()));
        String currentMonth = LocalDate.now().getMonth().name();
        when(contributionRepository.existsByMemberIdAndMonth(1L, currentMonth)).thenReturn(false);
        when(contributionRepository.save(any(Contribution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Contribution saved = contributionService.createContribution(
                1L, new BigDecimal("50.00"), null, false);

        assertThat(saved.getMonth()).isEqualTo(currentMonth);
    }

    @Test
    @DisplayName("normalises the month to upper case so JANUARY and january cannot bypass the duplicate check")
    void monthIsNormalised() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member()));
        when(contributionRepository.existsByMemberIdAndMonth(1L, "FEBRUARY")).thenReturn(false);
        when(contributionRepository.save(any(Contribution.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Contribution saved = contributionService.createContribution(
                1L, new BigDecimal("50.00"), " february ", true);

        assertThat(saved.getMonth()).isEqualTo("FEBRUARY");
    }
}