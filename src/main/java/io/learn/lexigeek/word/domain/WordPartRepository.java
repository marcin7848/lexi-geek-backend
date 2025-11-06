package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface WordPartRepository extends UUIDAwareJpaRepository<WordPart, Long> {
}

