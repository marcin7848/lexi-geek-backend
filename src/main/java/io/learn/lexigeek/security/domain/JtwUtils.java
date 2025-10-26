package io.learn.lexigeek.security.domain;

import io.jsonwebtoken.Jwts;
import lombok.experimental.UtilityClass;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@UtilityClass
class JtwUtils {

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
}
