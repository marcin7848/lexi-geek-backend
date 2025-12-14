package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface TaskScheduleRepository extends UUIDAwareJpaRepository<TaskSchedule, Long> {

    Optional<TaskSchedule> findByAccountId(final Long accountId);
}
