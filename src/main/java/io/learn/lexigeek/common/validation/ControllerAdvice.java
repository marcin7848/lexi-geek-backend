package io.learn.lexigeek.common.validation;

import io.learn.lexigeek.common.exception.AlreadyExistsException;
import io.learn.lexigeek.common.exception.AuthorizationException;
import io.learn.lexigeek.common.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
class ControllerAdvice {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorDto notFoundException(final NotFoundException e) {
        log.warn("{} - {}", e.getMessage(), e.getArgs());
        return new ErrorDto(e.getError(), e.getArgs());
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorDto alreadyExistsException(final AlreadyExistsException e) {
        log.warn("{} - {}", e.getMessage(), e.getArgs());
        return new ErrorDto(e.getError(), e.getArgs());
    }

    @ExceptionHandler(AuthorizationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ErrorDto invalidCredentialsException(final AuthorizationException e) {
        log.warn("{} - {}", e.getMessage(), e.getArgs());
        return new ErrorDto(e.getError(), e.getArgs());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorDto constraintValidationException(final ConstraintViolationException e) {
        log.warn(e.getMessage());
        return ErrorDto.from(ErrorCodes.VALIDATION_ERROR, List.of(), e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorDto methodArgumentNotValidException(final MethodArgumentNotValidException e) {
        log.warn(e.getMessage());
        return ErrorDto.from(ErrorCodes.VALIDATION_ERROR, List.of(), e);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ErrorDto exception(final Exception e) {
        log.warn(e.getMessage(), e);
        return new ErrorDto(ErrorCodes.GENERAL_ERROR, List.of());
    }
}
