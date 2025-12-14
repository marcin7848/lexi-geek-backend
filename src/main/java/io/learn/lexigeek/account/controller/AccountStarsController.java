package io.learn.lexigeek.account.controller;

import io.learn.lexigeek.account.AccountFacade;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@RequestMapping
class AccountStarsController {

    private static final class Routes {
        private static final String STARS = "/account/stars";
    }

    private final AccountFacade accountFacade;

    @GetMapping(Routes.STARS)
    Integer getStars() {
        return accountFacade.getStars();
    }
}
