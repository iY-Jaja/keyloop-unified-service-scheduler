package com.keyloop.scheduler.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity(name = "Vehicle")
@Table(name = "vehicle")
public class JpaVehicleEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private JpaCustomerEntity customer;

    @Column(nullable = false, unique = true)
    private String vin;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    protected JpaVehicleEntity() {
    }

    public JpaVehicleEntity(String id, JpaCustomerEntity customer, String vin,
                            String make, String model, int modelYear) {
        this.id = id;
        this.customer = customer;
        this.vin = vin;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
    }

    public String getId() {
        return id;
    }

    public JpaCustomerEntity getCustomer() {
        return customer;
    }

    public String getVin() {
        return vin;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public int getModelYear() {
        return modelYear;
    }
}
