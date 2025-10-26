package io.learn.lexigeek.common.exception;

import io.learn.lexigeek.common.validation.ErrorCodes;

public class AuthorizationException extends ErrorDtoException {
    public AuthorizationException(final ErrorCodes error, final Throwable cause, final Object... args) {
        super(error, cause, args);
    }
}
