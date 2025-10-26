package io.learn.lexigeek.security.domain;

import io.learn.lexigeek.security.AuthFacade;
import io.learn.lexigeek.security.dto.LoginForm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class AuthService implements AuthFacade {

    @Override
    public void login(final LoginForm form, final HttpServletResponse response) {
        final String token = JwtUtils.generateToken(form.email(), 3600);;
        JwtUtils.setAuthCookie(response, token, 3600);
    }
}
