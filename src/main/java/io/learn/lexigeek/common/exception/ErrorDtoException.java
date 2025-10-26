package io.learn.lexigeek.common.exception;

import io.learn.lexigeek.common.validation.ErrorCodes;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
abstract class ErrorDtoException extends RuntimeException {

    private final ErrorCodes error;
    private final List<Object> args;

    ErrorDtoException(final ErrorCodes error, final Throwable cause, final Object... args) {
        super(cause);
        this.error = error;
        this.args = Arrays.asList(args);
    }
}
