package io.learn.lexigeek.common.exception;

import io.learn.lexigeek.common.validation.ErrorCodes;

public class ValidationException extends ErrorDtoException {
    public ValidationException(final ErrorCodes error, final Throwable cause, final Object... args) {
        super(error, cause, args);
    }

    public ValidationException(final ErrorCodes error, final Object... args) {
        super(error, args);
    }
}

