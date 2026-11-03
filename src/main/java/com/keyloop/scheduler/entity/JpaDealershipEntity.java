package com.keyloop.scheduler.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity(name = "Dealership")
@Table(name = "dealership")
public class JpaDealershipEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String timezone;

    protected JpaDealershipEntity() {
    }

    public JpaDealershipEntity(String id, String name, String timezone) {
        this.id = id;
        this.name = name;
        this.timezone = timezone;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTimezone() {
        return timezone;
    }
}
