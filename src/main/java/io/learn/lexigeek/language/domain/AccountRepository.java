package io.learn.lexigeek.language.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.stereotype.Repository;

@Repository("LanguageAccountRepository")
interface AccountRepository extends UUIDAwareJpaRepository<Account, Long> {
}
