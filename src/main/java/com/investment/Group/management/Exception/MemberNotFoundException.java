package com.investment.Group.management.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a member referenced by a request does not exist.
 *
 * <p>Annotated with {@code @ResponseStatus(HttpStatus.NOT_FOUND)} so that Spring
 * maps it to HTTP 404. Throwing a bare {@code RuntimeException} instead
 * produces a 500, which falsely tells the client the server is broken when the
 * real problem is an invalid path variable.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(Long memberId) {
        super("No member found with id " + memberId);
    }
}