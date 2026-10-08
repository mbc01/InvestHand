package com.investment.Group.management.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a member attempts to record a second contribution for a month
 * that has already been paid.
 *
 * <p>Maps to HTTP 409 Conflict: the request is syntactically valid, but it
 * conflicts with the current state of the resource. A 500 would be misleading
 * and would also fill the logs with stack traces for ordinary user error.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateContributionException extends RuntimeException {

    public DuplicateContributionException(Long memberId, String month) {
        super("Member " + memberId + " has already made a contribution for " + month);
    }
}