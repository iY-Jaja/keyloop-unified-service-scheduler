package com.keyloop.scheduler.domain;

public class Technician {

    private final String id;
    private final String dealershipId;
    private final String name;

    public Technician(String id, String dealershipId, String name) {
        this.id = id;
        this.dealershipId = dealershipId;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getDealershipId() {
        return dealershipId;
    }

    public String getName() {
        return name;
    }
}
