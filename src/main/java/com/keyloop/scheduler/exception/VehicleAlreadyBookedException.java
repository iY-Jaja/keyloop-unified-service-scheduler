package com.keyloop.scheduler.exception;

public class VehicleAlreadyBookedException extends RuntimeException {

    public VehicleAlreadyBookedException() {
        super("The vehicle already has an overlapping confirmed appointment.");
    }
}
