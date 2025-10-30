package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface LanguageRepository extends UUIDAwareJpaRepository<Language, Long> {
    List<Language> findAllByAccountId(final Long accountId);

    Optional<Language> findByUuidAndAccountId(final UUID uuid, final Long accountId);
}
