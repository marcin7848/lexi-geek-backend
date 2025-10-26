package io.learn.lexigeek.security.domain;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@UtilityClass
class JwtUtils {

    static final String COOKIE_NAME = "JWT";
    private final SecretKey key = Jwts.SIG.HS256.key().build();

    String generateToken(final String subject, final long expiresInSeconds) {
        final Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(key)
                .compact();
    }

    Optional<String> extractSubject(final String token) {
        try {
            final String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Optional.ofNullable(subject);
        } catch (final Exception e) {
            return Optional.empty();
        }
    }


    public void setAuthCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        if (response == null || token == null || token.isBlank()) {
            return;
        }

        final String header = String.format("%s=%s; Max-Age=%d; Path=/; HttpOnly; Secure; SameSite=Lax", COOKIE_NAME, token, maxAgeSeconds);
        response.addHeader("Set-Cookie", header);
    }
}
