package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.entity.JpaCustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCustomerRepository extends JpaRepository<JpaCustomerEntity, String> {
}
