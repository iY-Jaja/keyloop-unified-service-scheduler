package com.keyloop.scheduler.repository;

import com.keyloop.scheduler.entity.JpaAppointmentEntity;
import com.keyloop.scheduler.domain.AppointmentStatus;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAppointmentRepository extends JpaRepository<JpaAppointmentEntity, String> {

    @Query("select case when count(a) > 0 then true else false end from Appointment a " +
            "where a.status = :status and a.dealership.id = :dealershipId " +
            "and a.serviceBay.id = :bayId and a.startAt < :endAt and a.endAt > :startAt")
    boolean existsBayConflict(@Param("status") AppointmentStatus status,
                              @Param("dealershipId") String dealershipId,
                              @Param("bayId") String bayId,
                              @Param("startAt") Instant startAt,
                              @Param("endAt") Instant endAt);

    @Query("select case when count(a) > 0 then true else false end from Appointment a " +
            "where a.status = :status and a.dealership.id = :dealershipId " +
            "and a.technician.id = :technicianId and a.startAt < :endAt and a.endAt > :startAt")
    boolean existsTechnicianConflict(@Param("status") AppointmentStatus status,
                                     @Param("dealershipId") String dealershipId,
                                     @Param("technicianId") String technicianId,
                                     @Param("startAt") Instant startAt,
                                     @Param("endAt") Instant endAt);

    @Query("select case when count(a) > 0 then true else false end from Appointment a " +
            "where a.status = :status and a.vehicle.id = :vehicleId " +
            "and a.startAt < :endAt and a.endAt > :startAt")
    boolean existsVehicleConflict(@Param("status") AppointmentStatus status,
                                  @Param("vehicleId") String vehicleId,
                                  @Param("startAt") Instant startAt,
                                  @Param("endAt") Instant endAt);
}
