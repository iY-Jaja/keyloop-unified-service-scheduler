package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.entity.JpaDealershipEntity;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaDealershipRepository extends JpaRepository<JpaDealershipEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Dealership d where d.id = :id")
    Optional<JpaDealershipEntity> findByIdForUpdate(@Param("id") String id);
}
