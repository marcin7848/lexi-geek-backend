package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("WordLanguageRepository")
interface LanguageRepository extends UUIDAwareJpaRepository<Language, Long> {

    @Query("SELECT l FROM WordLanguage l WHERE l.uuid = :uuid")
    Optional<Language> findByUuid(@Param("uuid") UUID uuid);
}

