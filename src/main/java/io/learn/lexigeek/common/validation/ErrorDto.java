package io.learn.lexigeek.common.validation;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static io.learn.lexigeek.common.validation.ValidationMessage.nameByCode;

record ErrorDto(ErrorCodes error, List<Object> args, List<FieldValidationMessage> validationMessages) {

    ErrorDto(final ErrorCodes error, final List<Object> args) {
        this(error, args, List.of());
    }

    record FieldValidationMessage(String field, String code, List<Object> args) {
    }

    static ErrorDto from(final ErrorCodes error, final List<Object> args, final ConstraintViolationException constraintViolationException) {
        final List<FieldValidationMessage> validationMessages = constraintViolationException.getConstraintViolations().stream().map(fe -> {
                    final String field = Arrays.stream(fe.getPropertyPath().toString().split("\\."))
                            .reduce((prev, next) -> next)
                            .orElse(null);
                    return new FieldValidationMessage(field, nameByCode(fe.getMessageTemplate()), List.of("FILL UP #TODO")); //#TODO
                })
                .toList();
        return new ErrorDto(error, args, validationMessages);
    }

    static ErrorDto from(final ErrorCodes error, final List<Object> args, final MethodArgumentNotValidException methodArgumentNotValidException) {
        final List<FieldValidationMessage> validationMessages = methodArgumentNotValidException.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    if (!Objects.equals(fe.getCode(), "Pattern")) {
                        return new FieldValidationMessage(fe.getField(), nameByCode(fe.getCode()), filterSimpleArgs(fe.getArguments()));
                    } else {
                        return new FieldValidationMessage(fe.getField(), nameByCode(fe.getDefaultMessage()), filterSimpleArgs(fe.getArguments()));

                    }
                })
                .toList();
        return new ErrorDto(error, args, validationMessages);
    }

    static ErrorDto from(final ErrorCodes error, final List<Object> args, final InvalidFormatException invalidFormatException) {
        final String field = invalidFormatException.getPath().getFirst().getPropertyName();
        final Object wrongValue = invalidFormatException.getValue();
        final List<Object> validValues = new ArrayList<>();

        if (invalidFormatException.getTargetType().isEnum()) {
            validValues.addAll(Arrays.asList(invalidFormatException.getTargetType().getEnumConstants()));
        }

        final List<Object> messageValues = Stream.concat(
                Stream.of(wrongValue),
                validValues.stream()
        ).toList();

        final FieldValidationMessage validationMessage = new FieldValidationMessage(
                field,
                "INVALID_FORMAT",
                messageValues
        );
        return new ErrorDto(error, args, List.of(validationMessage));
    }

    static ErrorDto from(final ErrorCodes error, final List<Object> args, final HttpMessageNotReadableException httpMessageNotReadableException) {
        if (httpMessageNotReadableException.getCause() instanceof InvalidFormatException invalidFormatException) {
            return from(error, args, invalidFormatException);
        }
        final String message = httpMessageNotReadableException.getMessage();
        final FieldValidationMessage validationMessage = new FieldValidationMessage(
                null,
                "JsonParseError",
                List.of(message != null ? message : "Invalid JSON format")
        );
        return new ErrorDto(error, args, List.of(validationMessage));
    }

    private static boolean isSimpleType(final Object o) {
        if (o == null) return true;
        final Class<?> c = o.getClass();
        return c.isPrimitive()
                || o instanceof String
                || o instanceof Number
                || o instanceof Boolean
                || o instanceof Character
                || c.isEnum();
    }

    private static List<Object> filterSimpleArgs(final Object[] args) {
        return (args == null)
                ? List.of()
                : Arrays.stream(args)
                .map(a -> (a instanceof Enum<?> e) ? e.name() : a)
                .filter(ErrorDto::isSimpleType)
                .toList();
    }
}
