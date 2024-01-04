package io.learn.lexigeek.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends UUIDAwareJpaRepository<Account, Long>, JpaSpecificationExecutor<Account> {
}
