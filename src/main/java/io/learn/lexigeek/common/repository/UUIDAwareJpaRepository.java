package io.learn.lexigeek.common.repository;

import io.learn.lexigeek.common.entity.AbstractUuidEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface UUIDAwareJpaRepository<T extends AbstractUuidEntity, ID> extends JpaRepository<T, ID> {

    Optional<T> findByUuid(final UUID uuid);

    void deleteByUuid(final UUID uuid);
}
