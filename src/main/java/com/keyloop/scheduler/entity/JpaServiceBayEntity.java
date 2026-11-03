package com.keyloop.scheduler.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "service_bay")
public class JpaServiceBayEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dealership_id", nullable = false)
    private JpaDealershipEntity dealership;

    @Column(nullable = false)
    private String name;

    protected JpaServiceBayEntity() {
    }

    public JpaServiceBayEntity(String id, JpaDealershipEntity dealership, String name) {
        this.id = id;
        this.dealership = dealership;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public JpaDealershipEntity getDealership() {
        return dealership;
    }

    public String getName() {
        return name;
    }
}
