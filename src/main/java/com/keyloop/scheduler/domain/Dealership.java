package com.keyloop.scheduler.domain;

public class Dealership {

    private final String id;
    private final String name;
    private final String timezone;

    public Dealership(String id, String name, String timezone) {
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
