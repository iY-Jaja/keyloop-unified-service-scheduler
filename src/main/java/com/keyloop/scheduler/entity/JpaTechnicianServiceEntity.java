package com.keyloop.scheduler.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity(name = "TechnicianService")
@Table(name = "technician_service",
        uniqueConstraints = @UniqueConstraint(name = "uq_technician_service",
                columnNames = {"technician_id", "service_type_id"}))
public class JpaTechnicianServiceEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id", nullable = false)
    private JpaTechnicianEntity technician;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_type_id", nullable = false)
    private JpaServiceTypeEntity serviceType;

    protected JpaTechnicianServiceEntity() {
    }

    public JpaTechnicianServiceEntity(String id, JpaTechnicianEntity technician,
                                      JpaServiceTypeEntity serviceType) {
        this.id = id;
        this.technician = technician;
        this.serviceType = serviceType;
    }

    public String getId() {
        return id;
    }

    public JpaTechnicianEntity getTechnician() {
        return technician;
    }

    public JpaServiceTypeEntity getServiceType() {
        return serviceType;
    }
}
