package io.learn.lexigeek.security.domain;

import io.learn.lexigeek.account.AccountFacade;
import io.learn.lexigeek.account.dto.AccountDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
    protected void doFilterInternal(final HttpServletRequest request,
                                    @Nullable final HttpServletResponse response,
                                    @Nullable final FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String token = extractToken(authHeader);
        if (token != null) {
            final Optional<String> subjectOpt = JtwUtils.extractSubject(token);
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

    private @Nullable String extractToken(@Nullable String headerValue) {
        if (headerValue == null) return null;
        String value = headerValue.trim();
        final String bearer = "Bearer ";
        while (value.length() >= bearer.length() && value.regionMatches(true, 0, bearer, 0, bearer.length())) {
            value = value.substring(bearer.length()).trim();
        }
        return value.isEmpty() ? null : value;
    }
}
