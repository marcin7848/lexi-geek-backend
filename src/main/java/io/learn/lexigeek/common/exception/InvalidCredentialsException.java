package io.learn.lexigeek.common.exception;

import io.learn.lexigeek.common.validation.ErrorCodes;

public class InvalidCredentialsException extends ErrorDtoException {
    public InvalidCredentialsException(final ErrorCodes error, final Throwable cause, final Object... args) {
        super(error, cause, args);
    }
}
