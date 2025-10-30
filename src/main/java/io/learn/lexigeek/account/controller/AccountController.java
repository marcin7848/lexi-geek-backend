package io.learn.lexigeek.account.controller;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
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

    private final AccountFacade accountFacade;

    @GetMapping(Routes.ACCOUNT)
    AccountDto getAccount() {
        return accountFacade.getLoggedAccount();
    }
}
