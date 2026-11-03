package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.entity.JpaServiceTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaServiceTypeRepository extends JpaRepository<JpaServiceTypeEntity, String> {
}
