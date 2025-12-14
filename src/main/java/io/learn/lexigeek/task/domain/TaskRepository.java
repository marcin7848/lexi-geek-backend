package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface TaskRepository extends UUIDAwareJpaRepository<Task, Long> {

    List<Task> findAllByAccountId(final Long accountId);

    void deleteAllByAccountId(final Long accountId);
}
