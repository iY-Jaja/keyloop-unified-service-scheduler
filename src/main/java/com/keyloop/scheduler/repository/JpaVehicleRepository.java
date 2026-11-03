package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.entity.JpaVehicleEntity;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaVehicleRepository extends JpaRepository<JpaVehicleEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :id")
    Optional<JpaVehicleEntity> findByIdForUpdate(@Param("id") String id);
}
