package io.learn.lexigeek.security.controller;

import io.learn.lexigeek.security.AuthFacade;
import io.learn.lexigeek.security.dto.LoginForm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class AuthController {

    private static final class Routes {
        private static final String LOGIN = "/login";
        private static final String LOGOUT = "/logout";
    }

    private final AuthFacade authFacade;

    @PostMapping(Routes.LOGIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void login(@RequestBody final LoginForm form, final HttpServletResponse response) {
        authFacade.login(form, response);
    }

    @PostMapping(value = Routes.LOGOUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(final HttpServletResponse response) {
        authFacade.logout(response);
    }
}
