package com.investment.Group.management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for creating a contribution.
 *
 * <p>Using a DTO instead of binding straight to the {@code Contribution}
 * entity means the client cannot set server-controlled fields such as
 * {@code id}, {@code status}, {@code paymentDate} or {@code member}. Previously
 * a caller could POST {@code {"status":"PAID"}} and overwrite the value the
 * server had just computed.
 *
 * @param amount monetary amount contributed; must be positive
 * @param month  optional month label; when omitted the current month is used
 */
public record ContributionRequest(

        @DecimalMin(value = "0.0", inclusive = false,
                message = "amount must be greater than zero")
        BigDecimal amount,

        @Size(max = 32, message = "month must be at most 32 characters")
        String month
) {
}