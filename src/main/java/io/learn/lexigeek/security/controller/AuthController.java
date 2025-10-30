package io.learn.lexigeek.security.controller;

import io.learn.lexigeek.account.dto.AccountForm;
import io.learn.lexigeek.security.AuthFacade;
import io.learn.lexigeek.security.dto.LoginForm;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class AuthController {

    private static final class Routes {
        private static final String LOGIN = "/login";
        private static final String LOGOUT = "/logout";
        private static final String REGISTER = "/register";
    }

    private final AuthFacade authFacade;

    @PostMapping(Routes.LOGIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void login(@RequestBody @Valid final LoginForm form, final HttpServletResponse response) {
        authFacade.login(form, response);
    }

    @PostMapping(value = Routes.LOGOUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(final HttpServletResponse response) {
        authFacade.logout(response);
    }

    @PostMapping(Routes.REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    void register(@RequestBody @Valid final AccountForm form) {
        authFacade.register(form);
    }
}
