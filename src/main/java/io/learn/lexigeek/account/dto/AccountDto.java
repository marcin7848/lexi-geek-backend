package io.learn.lexigeek.account.dto;

import java.util.UUID;

public record AccountDto(UUID uuid,
                         String username,
                         String email,
                         String password) {
}