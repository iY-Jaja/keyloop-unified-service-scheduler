package com.keyloop.scheduler.domain;

public class Vehicle {

    private final String id;
    private final String customerId;
    private final String vin;
    private final String make;
    private final String model;

    public Vehicle(String id, String customerId, String vin, String make, String model) {
        this.id = id;
        this.customerId = customerId;
        this.vin = vin;
        this.make = make;
        this.model = model;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
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
}
