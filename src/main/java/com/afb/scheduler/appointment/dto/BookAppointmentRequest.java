package com.afb.scheduler.appointment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record BookAppointmentRequest(

        @JsonProperty("refRDV")
        String refRdv,

        @JsonProperty("refClient")
        @NotBlank
        String refClient,

        @JsonProperty("secondClientRef")
        String secondClientRef,

        @JsonProperty("refService")
        @NotBlank
        String refService,

        @JsonProperty("refResponsable")
        @NotBlank
        String refResponsable,

        @JsonProperty("dateRDV")
        @NotNull
        @Future
        LocalDateTime dateRdv,

        @JsonProperty("motifRdv")
        String motifRdv
) {
}
