package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface LanguageRepository extends UUIDAwareJpaRepository<Language, Long>, JpaSpecificationExecutor<Language> {
    Optional<Language> findByUuidAndAccountId(final UUID uuid, final Long accountId); //TODO: to remove probably
}
