package io.learn.lexigeek.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountForm(@NotNull @Size(min = 3, max = 20) String username,
                          @NotNull @Email @Size(max = 255) String email,
                          @NotNull @Size(min = 6, max = 72) String password) {
}
