package io.learn.lexigeek.common.exception;

import io.learn.lexigeek.common.validation.ErrorCodes;

public class NotFoundException extends ErrorDtoException {
    public NotFoundException(final ErrorCodes error, final Throwable cause, final Object... args) {
        super(error, cause, args);
    }
}
