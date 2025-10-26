package io.learn.lexigeek.security.dto;

import jakarta.validation.constraints.*;

public record LoginForm(@NotNull @Email @Size(max = 255) String email,
                        @NotNull @Size(min = 6, max = 72) String password,
                        boolean rememberMe) {
}
