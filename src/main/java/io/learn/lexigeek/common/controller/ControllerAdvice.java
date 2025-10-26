package io.learn.lexigeek.common.controller;

import io.learn.lexigeek.common.exception.InvalidCredentialsException;
import io.learn.lexigeek.common.exception.NotFoundException;
import io.learn.lexigeek.common.validation.ErrorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
class ControllerAdvice {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorDto notFoundException(final NotFoundException exception) {
        log.warn(exception.getMessage(), exception);
        return new ErrorDto(exception.getError().name(), exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ErrorDto invalidCredentialsException(final InvalidCredentialsException exception) {
        log.warn(exception.getMessage(), exception);
        return new ErrorDto(exception.getError().name(), exception.getMessage());
    }
}
