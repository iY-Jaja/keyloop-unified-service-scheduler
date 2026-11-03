package com.keyloop.scheduler.dto;

import java.time.OffsetDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class BookingRequest {

    @NotBlank
    @Size(max = 64)
    public String dealershipId;

    @NotBlank
    @Size(max = 64)
    public String customerId;

    @NotBlank
    @Size(max = 64)
    public String vehicleId;

    @NotBlank
    @Size(max = 64)
    public String serviceTypeId;

    @NotNull
    public OffsetDateTime startAt;
}
