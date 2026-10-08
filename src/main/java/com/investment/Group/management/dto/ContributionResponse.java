package com.investment.Group.management.dto;

import com.investment.Group.management.model.Contribution;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response projection for a contribution.
 *
 * <p>Returning a record rather than the entity keeps the API contract
 * explicit and independent of the JPA model, so adding a field to
 * {@code Contribution} can never silently start leaking it over HTTP.
 */
public record ContributionResponse(
        Long id,
        BigDecimal amount,
        LocalDate paymentDate,
        String month,
        String status,
        Long memberId
) {

    public static ContributionResponse from(Contribution contribution) {
        return new ContributionResponse(
                contribution.getId(),
                contribution.getAmount(),
                contribution.getPaymentDate(),
                contribution.getMonth(),
                contribution.getStatus(),
                contribution.getMember() == null ? null : contribution.getMember().getId()
        );
    }
}