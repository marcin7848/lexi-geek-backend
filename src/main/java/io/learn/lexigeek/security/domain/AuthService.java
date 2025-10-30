package io.learn.lexigeek.security.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.common.exception.AuthorizationException;
import io.learn.lexigeek.security.AuthFacade;
import io.learn.lexigeek.security.dto.LoginForm;
import io.learn.lexigeek.account.dto.AccountForm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import static io.learn.lexigeek.common.validation.ErrorCodes.INVALID_CREDENTIALS;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class AuthService implements AuthFacade {

    private static final int ACCESS_TTL_SECONDS = 15 * 60;
    private static final int REFRESH_TTL_SECONDS = 30 * 24 * 60 * 60;

    private final AuthenticationManager authenticationManager;
    private final AccountFacade accountFacade;

    @Override
    public void login(final LoginForm form, final HttpServletResponse response) {
        try {
            final Authentication authRequest = new UsernamePasswordAuthenticationToken(form.email(), form.password());
            authenticationManager.authenticate(authRequest);

            final String accessToken = JwtUtils.generateToken(form.email(), ACCESS_TTL_SECONDS);
            JwtUtils.setAccessCookie(response, accessToken, ACCESS_TTL_SECONDS);

            if (form.rememberMe()) {
                final String refreshToken = JwtUtils.generateToken(form.email(), REFRESH_TTL_SECONDS);
                JwtUtils.setRefreshCookie(response, refreshToken, REFRESH_TTL_SECONDS);
            } else {
                JwtUtils.clearCookie(response, JwtUtils.REFRESH_COOKIE_NAME);
            }
        } catch (final Exception e) {
            throw new AuthorizationException(INVALID_CREDENTIALS, e);
        }
    }

    @Override
    public void logout(final HttpServletResponse response) {
        JwtUtils.clearAuthCookies(response);
    }

    @Override
    public void register(final AccountForm form) {
        accountFacade.createAccount(form);
    }
}
