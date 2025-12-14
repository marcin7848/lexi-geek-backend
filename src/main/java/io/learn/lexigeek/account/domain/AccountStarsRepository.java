package io.learn.lexigeek.account.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
interface AccountStarsRepository extends UUIDAwareJpaRepository<AccountStars, Long> {

    @Query("SELECT COALESCE(SUM(a.stars), 0) FROM AccountStars a WHERE a.account = :account")
    Integer getTotalStarsByAccount(@Param("account") final Account account);

    List<AccountStars> findByAccountAndCreatedBetweenOrderByCreatedAsc(final Account account,
                                                                       final LocalDateTime start,
                                                                       final LocalDateTime end);
}
