package com.keyloop.scheduler.service;

import com.keyloop.scheduler.persistence.JpaEntityMapper;
import com.keyloop.scheduler.repository.JpaAppointmentRepository;
import com.keyloop.scheduler.domain.Appointment;
import com.keyloop.scheduler.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentQueryService {

    private final JpaAppointmentRepository appointmentRepository;
    private final JpaEntityMapper mapper;

    public AppointmentQueryService(JpaAppointmentRepository appointmentRepository, JpaEntityMapper mapper) {
        this.appointmentRepository = appointmentRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Appointment get(String appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new NotFoundException("Appointment not found."));
    }

    @Transactional(readOnly = true)
    public Page<Appointment> list(int page, int size) {
        Sort sort = Sort.by(Sort.Order.asc("startAt"), Sort.Order.asc("id"));
        return appointmentRepository.findAll(PageRequest.of(page, size, sort))
                .map(mapper::toDomain);
    }
}
