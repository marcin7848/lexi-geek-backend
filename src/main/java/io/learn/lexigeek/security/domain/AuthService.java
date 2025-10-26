package io.learn.lexigeek.security.domain;

import io.learn.lexigeek.security.AuthFacade;
import io.learn.lexigeek.security.dto.LoginDto;
import io.learn.lexigeek.security.dto.LoginForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class AuthService implements AuthFacade {

    private final AuthenticationManager authenticationManager;

    @Override
    public LoginDto login(final LoginForm form) {
        final Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(form.email(), form.password())
        );
        String token = JtwUtils.generateToken(form.email(), 3600);
        return new LoginDto(token);
    }
}
