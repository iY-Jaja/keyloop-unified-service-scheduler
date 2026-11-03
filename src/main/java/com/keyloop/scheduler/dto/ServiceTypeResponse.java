package com.keyloop.scheduler.dto;

import com.keyloop.scheduler.domain.ServiceType;

public class ServiceTypeResponse {

    public final String id;
    public final String name;
    public final int durationMinutes;

    public ServiceTypeResponse(ServiceType serviceType) {
        this.id = serviceType.getId();
        this.name = serviceType.getName();
        this.durationMinutes = serviceType.getDurationMinutes();
    }
}
