package com.investment.Group.management.Service;

import com.investment.Group.management.Exception.DuplicateContributionException;
import com.investment.Group.management.Exception.MemberNotFoundException;
import com.investment.Group.management.model.Contribution;
import com.investment.Group.management.model.Member;
import com.investment.Group.management.Repository.ContributionRepository;
import com.investment.Group.management.Repository.MemberRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for recording and querying member contributions.
 */
@Service
@Transactional(readOnly = true)
public class ContributionService {

    /**
     * Contributions are considered on time up to and including this day of
     * month. Anything later is recorded as LATE.
     */
    public static final int PAYMENT_DUE_DAY_OF_MONTH = 5;

    static final String STATUS_PAID = "PAID";
    static final String STATUS_LATE = "LATE";

    private final ContributionRepository contributionRepository;
    private final MemberRepository memberRepository;

    public ContributionService(ContributionRepository contributionRepository,
                               MemberRepository memberRepository) {
        this.contributionRepository = contributionRepository;
        this.memberRepository = memberRepository;
    }

    /**
     * Records a contribution for a member.
     *
     * <p>The transaction matters here: the duplicate check and the insert must
     * be atomic. Without {@code @Transactional} (and the row lock the write
     * implies) two simultaneous requests for the same month can both pass the
     * exists-check and insert duplicate rows.
     *
     * @param memberId      the member making the contribution
     * @param amount        amount contributed; must not be null or negative
     * @param month         contribution month, e.g. "JANUARY"
     * @param monthAssignedByClient whether {@code month} was supplied by the
     *                              caller. When false, the current month is
     *                              used instead.
     * @throws MemberNotFoundException      if no such member exists (404)
     * @throws DuplicateContributionException if already paid this month (409)
     * @throws IllegalArgumentException     if the amount is missing or negative (400)
     */
    @Transactional
    public Contribution createContribution(Long memberId, BigDecimal amount,
                                           String month, boolean monthAssignedByClient) {

        if (memberId == null) {
            throw new IllegalArgumentException("memberId is required");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is required");
        }
        // Comparing with compareTo, not equals: equals() on BigDecimal is
        // scale sensitive, so new BigDecimal("100.00").equals(new
        // BigDecimal("100")) is false even though both are worth 100.
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        String effectiveMonth = (monthAssignedByClient && month != null && !month.isBlank())
                ? month.trim().toUpperCase()
                : LocalDate.now().getMonth().name();

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));

        if (contributionRepository.existsByMemberIdAndMonth(memberId, effectiveMonth)) {
            throw new DuplicateContributionException(memberId, effectiveMonth);
        }

        Contribution contribution = new Contribution();
        contribution.setAmount(amount);
        contribution.setMonth(effectiveMonth);
        contribution.setPaymentDate(LocalDate.now());
        contribution.setStatus(statusFor(LocalDate.now()));
        contribution.setMember(member);

        return contributionRepository.save(contribution);
    }

    private String statusFor(LocalDate paymentDate) {
        return paymentDate.getDayOfMonth() > PAYMENT_DUE_DAY_OF_MONTH ? STATUS_LATE : STATUS_PAID;
    }

    public List<Contribution> getAllContributions() {
        return contributionRepository.findAll();
    }

    public List<Contribution> getContributionsByMember(Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("memberId is required");
        }
        return contributionRepository.findByMemberId(memberId);
    }
}