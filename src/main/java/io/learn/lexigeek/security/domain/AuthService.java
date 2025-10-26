package io.learn.lexigeek.security.domain;

import io.learn.lexigeek.security.AuthFacade;
import io.learn.lexigeek.security.dto.LoginDto;
import io.learn.lexigeek.security.dto.LoginForm;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class AuthService implements AuthFacade {

    @Override
    public LoginDto login(final LoginForm form) {
        final String token = JtwUtils.generateToken(form.email(), 3600);
        return new LoginDto("Bearer " + token);
    }
}
