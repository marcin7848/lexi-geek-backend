package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface TaskRepository extends UUIDAwareJpaRepository<Task, Long> {

    List<Task> findAllByAccountId(final Long accountId);

    Optional<Task> findByUuidAndAccountId(final UUID uuid, final Long accountId);

    void deleteAllByAccountId(final Long accountId);
}
