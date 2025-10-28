package io.learn.lexigeek.security;

import io.learn.lexigeek.security.dto.LoginForm;
import io.learn.lexigeek.account.dto.AccountForm;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthFacade {

    void login(final LoginForm form, final HttpServletResponse response);

    void logout(final HttpServletResponse response);

    void register(final AccountForm form);
}
