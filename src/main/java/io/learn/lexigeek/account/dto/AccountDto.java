package io.learn.lexigeek.account.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

public record AccountDto(@JsonIgnore Long id,
                         UUID uuid,
                         String username,
                         String email,
                         @JsonIgnore String password) {
}