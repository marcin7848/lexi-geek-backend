package io.learn.lexigeek.common.validation;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
@Getter
public enum ValidationMessage {

    MUST_NOT_BE_BLANK("NotBlank"),
    MUST_NOT_BE_NULL("NotNull"),
    WRONG_EMAIL("Email"),
    WRONG_SIZE("Size"),
    TYPE_MISMATCH("typeMismatch"),
    BOTH_MUST_NOT_BE_NULL("bothMustNotBeNull"),
    MIN("Min"),
    MAX("Max"),
    UPPER_CASE_ONLY("UPPER_CASE_ONLY");

    private final String code;

    private static final Map<String, String> messages = Arrays.stream(values())
            .collect(toMap(x -> x.code, Enum::name));

    public static String nameByCode(final String code) {
        return messages.getOrDefault(code, "UNKNOWN_VALIDATION_EXCEPTION");
    }
}


