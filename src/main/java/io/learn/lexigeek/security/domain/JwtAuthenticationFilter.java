package io.learn.lexigeek.security.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AccountFacade accountFacade;

    @Override
    protected void doFilterInternal(@Nullable final HttpServletRequest request,
                                    @Nullable final HttpServletResponse response,
                                    @Nullable final FilterChain filterChain) throws ServletException, IOException {
        if (request == null) return;
        final String token = extractTokenFromCookies(request);
        if (token != null) {
            final Optional<String> subjectOpt = JwtUtils.extractSubject(token);
            if (subjectOpt.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                final String email = subjectOpt.get();
                final AccountDto account = accountFacade.getAccountByEmail(email);
                final UserDetails userDetails = new User(account.email(), account.password(), Collections.singletonList(new SimpleGrantedAuthority("USER")));
                final UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        if (filterChain != null) {
            filterChain.doFilter(request, response);
        }
    }

    private @Nullable String extractTokenFromCookies(HttpServletRequest request) {
        final Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (final Cookie c : cookies) {
            if (JwtUtils.COOKIE_NAME.equals(c.getName())) {
                final String v = c.getValue();
                return (v == null || v.isBlank()) ? null : v;
            }
        }
        return null;
    }
}
