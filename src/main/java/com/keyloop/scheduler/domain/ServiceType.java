package com.keyloop.scheduler.domain;

public class ServiceType {

    private final String id;
    private final String name;
    private final int durationMinutes;

    public ServiceType(String id, String name, int durationMinutes) {
        this.id = id;
        this.name = name;
        this.durationMinutes = durationMinutes;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }
}
