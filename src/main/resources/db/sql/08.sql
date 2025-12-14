--liquibase formatted sql
--changeset marcin.kaczor:8 labels:LG-8

CREATE TABLE account_stars
(
    id         BIGINT PRIMARY KEY,
    uuid       UUID                        NOT NULL UNIQUE,
    created    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    stars      INTEGER                     NOT NULL,
    account_id BIGINT                      NOT NULL
        CONSTRAINT fk_account_stars_account REFERENCES accounts (id) ON DELETE CASCADE
);

CREATE INDEX idx_account_stars_account ON account_stars (account_id);
CREATE INDEX idx_account_stars_created ON account_stars (created);

--rollback DROP TABLE "account_stars";
