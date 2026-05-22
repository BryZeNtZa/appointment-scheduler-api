package com.afb.scheduler.appointment.dto;

import com.afb.scheduler.appointment.Appointment;
import com.afb.scheduler.appointment.AppointmentStatus;
import com.afb.scheduler.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

public record AppointmentResponse(

        @JsonProperty("refRDV")
        String refRdv,

        @JsonProperty("refResponsable")
        String refResponsable,

        @JsonProperty("refService")
        String refService,

        @JsonProperty("clientRefs")
        List<String> clientRefs,

        @JsonProperty("dateRDV")
        LocalDateTime dateRdv,

        @JsonProperty("motifRdv")
        String motifRdv,

        @JsonProperty("status")
        AppointmentStatus status
) {

    public static AppointmentResponse from(Appointment appointment) {
        List<String> clientRefs = appointment.getParticipants().stream()
                .map(User::getRef)
                .toList();
        return new AppointmentResponse(
                appointment.getRefRdv(),
                appointment.getManager().getRef(),
                appointment.getDepartment().getCode(),
                clientRefs,
                appointment.getDateRdv(),
                appointment.getMotifRdv(),
                appointment.getStatus()
        );
    }
}
