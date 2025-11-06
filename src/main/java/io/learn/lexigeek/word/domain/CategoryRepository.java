package io.learn.lexigeek.word.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("WordCategoryRepository")
interface CategoryRepository extends UUIDAwareJpaRepository<Category, Long> {

    @Query("SELECT c FROM WordCategory c WHERE c.uuid = :uuid AND c.language.uuid = :languageUuid")
    Optional<Category> findByUuidAndLanguageUuid(@Param("uuid") final UUID uuid,
                                                 @Param("languageUuid") final UUID languageUuid);
}
