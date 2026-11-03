package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.entity.JpaTechnicianEntity;
import com.keyloop.scheduler.entity.JpaTechnicianServiceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaTechnicianServiceRepository extends JpaRepository<JpaTechnicianServiceEntity, String> {

    @Query("select ts.technician from TechnicianService ts " +
            "where ts.serviceType.id = :serviceTypeId " +
            "and ts.technician.dealership.id = :dealershipId " +
            "order by ts.technician.id")
    List<JpaTechnicianEntity> findQualifiedTechnicians(@Param("dealershipId") String dealershipId,
                                                       @Param("serviceTypeId") String serviceTypeId);
}
