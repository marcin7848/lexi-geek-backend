package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import io.learn.lexigeek.language.dto.ShortcutDto;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface LanguageRepository extends UUIDAwareJpaRepository<Language, Long>, JpaSpecificationExecutor<Language> {
    Optional<Language> findByUuidAndAccountId(final UUID uuid, final Long accountId);

    @Query("SELECT new io.learn.lexigeek.language.dto.ShortcutDto(l.name, l.shortcut, CAST(COUNT(l.shortcut) AS int)) " +
            "FROM Language l " +
            "WHERE (:shortcut IS NULL OR :shortcut = '' OR LOWER(l.shortcut) LIKE LOWER(CONCAT('%', :shortcut, '%'))) " +
            "GROUP BY l.name, l.shortcut " +
            "ORDER BY COUNT(l.shortcut) DESC")
    List<ShortcutDto> findPopularShortcuts(@Param("shortcut") final String shortcut);
}
