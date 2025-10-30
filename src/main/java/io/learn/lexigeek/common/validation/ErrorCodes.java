package io.learn.lexigeek.common.validation;

public enum ErrorCodes {
    //ACCOUNT
    EMAIL_ALREADY_EXISTS,
    USER_NOT_FOUND,

    //LANGUAGE
    LANGUAGE_NOT_FOUND,

    //GENERAL
    EXTERNAL_SERVICE_ERROR,
    GENERAL_ERROR,
    INTERNAL_SERVER_ERROR,
    VALIDATION_ERROR,

    //SECURITY
    INVALID_CREDENTIALS,
}
