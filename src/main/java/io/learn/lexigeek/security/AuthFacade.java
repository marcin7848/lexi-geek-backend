package io.learn.lexigeek.security;

import io.learn.lexigeek.security.dto.LoginDto;
import io.learn.lexigeek.security.dto.LoginForm;

public interface AuthFacade {

    LoginDto login(LoginForm form);
}
