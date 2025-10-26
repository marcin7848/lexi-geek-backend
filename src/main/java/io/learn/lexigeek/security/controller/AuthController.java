package io.learn.lexigeek.security.controller;

import io.learn.lexigeek.security.AuthFacade;
import io.learn.lexigeek.security.dto.LoginDto;
import io.learn.lexigeek.security.dto.LoginForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class AuthController {

    private static final class Routes {
        private static final String LOGIN = "/login";
    }

    private final AuthFacade authFacade;

    @PostMapping(Routes.LOGIN)
    LoginDto login(@RequestBody final LoginForm form) {
        return authFacade.login(form);
    }
}
