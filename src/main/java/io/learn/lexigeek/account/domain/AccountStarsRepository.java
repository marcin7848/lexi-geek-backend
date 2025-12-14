package io.learn.lexigeek.account.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface AccountStarsRepository extends UUIDAwareJpaRepository<AccountStars, Long> {
}
