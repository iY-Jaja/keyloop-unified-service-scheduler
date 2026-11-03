package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.entity.JpaTechnicianEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTechnicianRepository extends JpaRepository<JpaTechnicianEntity, String> {

    List<JpaTechnicianEntity> findByDealership_IdOrderByIdAsc(String dealershipId);
}
