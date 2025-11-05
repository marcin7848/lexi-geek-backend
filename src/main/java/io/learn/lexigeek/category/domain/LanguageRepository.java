package io.learn.lexigeek.category.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

@Repository("CategoryLanguageRepository")
interface LanguageRepository extends UUIDAwareJpaRepository<Language, Long> {
}

