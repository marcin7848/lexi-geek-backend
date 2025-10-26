package io.learn.lexigeek.common.validation;

import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.learn.lexigeek.common.validation.ValidationMessage.nameByCode;

public record ErrorDto(String code, String error, List<FieldValidationMessage> validationMessages) {

    public ErrorDto(final String code, final String error, final String field, final String message) {
        this(code, error, List.of(new FieldValidationMessage(field, null, message)));
    }

    public ErrorDto(final String code, final String error) {
        this(code, error, List.of());
    }

    public record FieldValidationMessage(String field, String code, String message) {
    }

    public static ErrorDto from(final String code, final String error, final ConstraintViolationException constraintViolationException) {
        final List<FieldValidationMessage> validationMessages = constraintViolationException.getConstraintViolations().stream().map(fe -> {
                    final String field = Arrays.stream(fe.getPropertyPath().toString().split("\\."))
                            .reduce((prev, next) -> next)
                            .orElse(null);
                    return new FieldValidationMessage(field, nameByCode(fe.getMessageTemplate()), fe.getMessage());
                })
                .toList();
        return new ErrorDto(code, error, validationMessages);
    }

    public static ErrorDto from(final String code, final String error, final MethodArgumentNotValidException methodArgumentNotValidException) {
        final List<FieldValidationMessage> validationMessages = methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    if (!Objects.equals(fe.getCode(), "Pattern")) {
                        return new FieldValidationMessage(fe.getField(), fe.getCode(), methodArgumentNotValidException.getMessage());
                    } else {
                        return new FieldValidationMessage(fe.getField(), nameByCode(fe.getDefaultMessage()), methodArgumentNotValidException.getMessage());

                    }
                })
                .toList();
        return new ErrorDto(code, error, validationMessages);
    }
}
