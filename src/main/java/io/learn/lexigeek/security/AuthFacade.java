package io.learn.lexigeek.security;

import io.learn.lexigeek.security.dto.LoginForm;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthFacade {

    void login(final LoginForm form, final HttpServletResponse response);
}
