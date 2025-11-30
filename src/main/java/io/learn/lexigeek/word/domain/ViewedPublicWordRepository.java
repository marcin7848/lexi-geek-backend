package io.learn.lexigeek.word.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ViewedPublicWordRepository extends JpaRepository<ViewedPublicWord, Long> {

    boolean existsByAccountIdAndWordId(final Long accountId, final Long wordId);
}
