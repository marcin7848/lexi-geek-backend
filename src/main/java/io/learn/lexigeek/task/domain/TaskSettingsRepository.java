package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface TaskSettingsRepository extends UUIDAwareJpaRepository<TaskSettings, Long> {

    List<TaskSettings> findAllByAccountId(final Long accountId);

    Optional<TaskSettings> findByLanguageIdAndAccountId(final Long languageId, final Long accountId);
}
