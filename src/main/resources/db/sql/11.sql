--liquibase formatted sql
--changeset marcin.kaczor:11 labels:LG-13

ALTER TABLE languages DROP COLUMN code_for_translator;

--rollback ALTER TABLE languages ADD COLUMN code_for_translator VARCHAR(10) NOT NULL DEFAULT '';
