package io.learn.lexigeek.activity.domain;

import io.learn.lexigeek.common.repository.UUIDAwareJpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
interface ActivityRepository extends UUIDAwareJpaRepository<Activity, Long>, JpaSpecificationExecutor<Activity> {
}
