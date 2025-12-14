package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface LanguageRepository extends UUIDAwareJpaRepository<Language, Long> {
}
