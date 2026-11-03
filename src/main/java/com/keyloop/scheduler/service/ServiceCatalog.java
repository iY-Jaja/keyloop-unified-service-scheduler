package com.keyloop.scheduler.service;

import com.keyloop.scheduler.domain.Dealership;
import com.keyloop.scheduler.persistence.JpaEntityMapper;
import com.keyloop.scheduler.repository.JpaDealershipRepository;
import com.keyloop.scheduler.repository.JpaServiceBayRepository;
import com.keyloop.scheduler.repository.JpaServiceTypeRepository;
import com.keyloop.scheduler.repository.JpaTechnicianRepository;
import com.keyloop.scheduler.dto.ResourceSummary;
import com.keyloop.scheduler.dto.ServiceTypeResponse;
import com.keyloop.scheduler.exception.NotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceCatalog {

    private final JpaServiceTypeRepository serviceTypeRepository;
    private final JpaDealershipRepository dealershipRepository;
    private final JpaServiceBayRepository serviceBayRepository;
    private final JpaTechnicianRepository technicianRepository;
    private final JpaEntityMapper mapper;

    public ServiceCatalog(JpaServiceTypeRepository serviceTypeRepository,
                          JpaDealershipRepository dealershipRepository,
                          JpaServiceBayRepository serviceBayRepository,
                          JpaTechnicianRepository technicianRepository,
                          JpaEntityMapper mapper) {
        this.serviceTypeRepository = serviceTypeRepository;
        this.dealershipRepository = dealershipRepository;
        this.serviceBayRepository = serviceBayRepository;
        this.technicianRepository = technicianRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "serviceTypes", key = "'all'")
    public List<ServiceTypeResponse> getServiceTypes() {
        return serviceTypeRepository.findAll().stream()
                .map(mapper::toDomain)
                .map(ServiceTypeResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dealershipResources", key = "#dealershipId")
    public ResourceSummary getResources(String dealershipId) {
        Dealership dealership = dealershipRepository.findById(dealershipId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new NotFoundException("Dealership not found."));
        return new ResourceSummary(dealership.getId(),
                serviceBayRepository.findByDealership_IdOrderByIdAsc(dealershipId).stream()
                        .map(mapper::toDomain)
                        .collect(Collectors.toList()),
                technicianRepository.findByDealership_IdOrderByIdAsc(dealershipId).stream()
                        .map(mapper::toDomain)
                        .collect(Collectors.toList()));
    }
}
