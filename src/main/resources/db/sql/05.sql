--liquibase formatted sql
--changeset marcin.kaczor:5 labels:LG-G

ALTER TABLE "word_stats"
    DROP COLUMN answered,
    DROP COLUMN to_answer,
    ADD COLUMN correct     BOOLEAN                     NOT NULL DEFAULT FALSE,
    ADD COLUMN answer_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

--rollback ALTER TABLE "word_stats" DROP COLUMN answer_time;
--rollback ALTER TABLE "word_stats" DROP COLUMN correct;
--rollback ALTER TABLE "word_stats" ADD COLUMN to_answer INTEGER NOT NULL DEFAULT 0;
--rollback ALTER TABLE "word_stats" ADD COLUMN answered INTEGER NOT NULL DEFAULT 0;

