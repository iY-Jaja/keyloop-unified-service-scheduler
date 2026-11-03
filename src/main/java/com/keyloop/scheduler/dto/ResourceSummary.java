package com.keyloop.scheduler.dto;

import com.keyloop.scheduler.domain.ServiceBay;
import com.keyloop.scheduler.domain.Technician;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceSummary {

    public final String dealershipId;
    public final List<Resource> serviceBays;
    public final List<Resource> technicians;

    public ResourceSummary(String dealershipId, List<ServiceBay> bays, List<Technician> technicians) {
        this.dealershipId = dealershipId;
        this.serviceBays = bays.stream()
                .map(bay -> new Resource(bay.getId(), bay.getName()))
                .collect(Collectors.toList());
        this.technicians = technicians.stream()
                .map(technician -> new Resource(technician.getId(), technician.getName()))
                .collect(Collectors.toList());
    }

    public static class Resource {
        public final String id;
        public final String name;

        public Resource(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
