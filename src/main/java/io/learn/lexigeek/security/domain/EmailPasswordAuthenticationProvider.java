package io.learn.lexigeek.security.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class EmailPasswordAuthenticationProvider implements AuthenticationProvider {

    private final AccountFacade accountFacade;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        final String email = authentication.getName();
        final String rawPassword = authentication.getCredentials() != null ? authentication.getCredentials().toString() : null;

        final AccountDto account = accountFacade.getAccountByEmail(email);
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, account.password())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("USER"))
        );
    }

    @Override
    public boolean supports(@Nullable final Class<?> authentication) {
        if (authentication == null) {
            return false;
        }
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
