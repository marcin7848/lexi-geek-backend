package io.learn.lexigeek.security.controller;

import io.learn.lexigeek.security.AuthFacade;
import io.learn.lexigeek.security.dto.LoginForm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class AuthController {

    private static final class Routes {
        private static final String LOGIN = "/login";
    }

    private final AuthFacade authFacade;

    @PostMapping(Routes.LOGIN)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void login(@RequestBody final LoginForm form, final HttpServletResponse response) {
        authFacade.login(form, response);
    }
}
