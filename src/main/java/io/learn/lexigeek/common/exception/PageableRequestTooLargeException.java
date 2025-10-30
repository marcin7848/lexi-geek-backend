package io.learn.lexigeek.common.exception;

import io.learn.lexigeek.common.validation.ErrorCodes;

public class PageableRequestTooLargeException extends ErrorDtoException {
    public PageableRequestTooLargeException(final ErrorCodes error, final Throwable cause, final Object... args) {
        super(error, cause, args);
    }

    public PageableRequestTooLargeException(final ErrorCodes error, final Object... args) {
        super(error, args);
    }
}
