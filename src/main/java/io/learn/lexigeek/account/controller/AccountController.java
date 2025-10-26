package io.learn.lexigeek.account.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class AccountController {
    private static final class Routes {
        private static final String ACCOUNT = "/account";
    }

    @GetMapping(Routes.ACCOUNT)
    String aaaa() {
        return "a";
    }
}
