package io.learn.lexigeek.task.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

@Repository("TaskAccountRepository")
interface AccountRepository extends UUIDAwareJpaRepository<Account, Long> {
}
