--liquibase formatted sql
--changeset marcin.kaczor:1 labels:LG-1

CREATE SEQUENCE accounts_sequence;

CREATE TABLE "accounts"
(
    id       BIGINT PRIMARY KEY,
    uuid     UUID UNIQUE  NOT NULL,
    username VARCHAR(255) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(72)  NOT NULL
);

--rollback DROP TABLE "accounts";
--rollback DROP SEQUENCE accounts_sequence;
