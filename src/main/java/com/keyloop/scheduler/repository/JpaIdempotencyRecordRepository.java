package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.entity.JpaIdempotencyRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaIdempotencyRecordRepository extends JpaRepository<JpaIdempotencyRecordEntity, String> {
}
