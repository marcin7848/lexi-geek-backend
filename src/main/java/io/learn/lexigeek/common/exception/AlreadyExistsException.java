package io.learn.lexigeek.common.exception;

import io.learn.lexigeek.common.validation.ErrorCodes;

public class AlreadyExistsException extends ErrorDtoException {
    public AlreadyExistsException(final ErrorCodes error, final Throwable cause, final Object... args) {
        super(error, cause, args);
    }

    public AlreadyExistsException(final ErrorCodes error, final Object... args) {
        super(error, args);
    }
}
