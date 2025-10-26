package io.learn.lexigeek.security.domain;

import io.learn.lexigeek.security.AuthFacade;
import io.learn.lexigeek.security.dto.LoginForm;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class AuthService implements AuthFacade {

    private final AuthenticationManager authenticationManager;

    @Override
    public void login(final LoginForm form, final HttpServletResponse response) {
        try {
            final Authentication authRequest = new UsernamePasswordAuthenticationToken(form.email(), form.password());
            authenticationManager.authenticate(authRequest);
            final String token = JwtUtils.generateToken(form.email(), 3600);
            JwtUtils.setAuthCookie(response, token, 3600);
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid credentials");
        }
    }
}
