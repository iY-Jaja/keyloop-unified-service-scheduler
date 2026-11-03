package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.entity.JpaServiceBayEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaServiceBayRepository extends JpaRepository<JpaServiceBayEntity, String> {

    List<JpaServiceBayEntity> findByDealership_IdOrderByIdAsc(String dealershipId);
}
