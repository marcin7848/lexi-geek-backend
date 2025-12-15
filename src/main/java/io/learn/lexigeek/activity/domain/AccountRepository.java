package io.learn.lexigeek.activity.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

@Repository("ActivityAccountRepository")
interface AccountRepository extends UUIDAwareJpaRepository<Account, Long> {
}
