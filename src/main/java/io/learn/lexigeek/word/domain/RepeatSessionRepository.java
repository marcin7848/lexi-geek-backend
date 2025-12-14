package io.learn.lexigeek.word.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface RepeatSessionRepository extends JpaRepository<RepeatSession, Long> {

    @Query("SELECT rs FROM RepeatSession rs WHERE rs.uuid = :uuid")
    Optional<RepeatSession> findByUuid(@Param("uuid") UUID uuid);

    @Query("SELECT rs FROM RepeatSession rs WHERE rs.language.uuid = :languageUuid")
    Optional<RepeatSession> findByLanguageUuid(@Param("languageUuid") UUID languageUuid);

    @Query("SELECT COUNT(rs) > 0 FROM RepeatSession rs WHERE rs.language.uuid = :languageUuid")
    boolean existsByLanguageUuid(@Param("languageUuid") UUID languageUuid);
}


