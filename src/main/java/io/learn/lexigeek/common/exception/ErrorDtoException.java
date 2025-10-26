package io.learn.lexigeek.common.exception;

import io.learn.lexigeek.common.validation.ErrorCodes;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public abstract class ErrorDtoException extends RuntimeException {

    private final ErrorCodes error;
    private final List<Object> args;

    public ErrorDtoException(final ErrorCodes error, final Throwable cause, final Object... args) {
        super(cause);
        this.error = error;
        this.args = Arrays.asList(args);
    }
}
