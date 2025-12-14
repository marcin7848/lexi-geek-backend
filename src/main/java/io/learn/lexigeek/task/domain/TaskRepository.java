package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import io.learn.lexigeek.task.dto.TaskType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
interface TaskRepository extends UUIDAwareJpaRepository<Task, Long> {

    List<Task> findAllByAccountId(final Long accountId);

    void deleteAllByAccountId(final Long accountId);

    Optional<Task> findByAccountIdAndType(final Long accountId, final TaskType type);
}
