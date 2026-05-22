package com.afb.scheduler.appointment;

import com.afb.scheduler.appointment.dto.AppointmentResponse;
import com.afb.scheduler.appointment.dto.BookAppointmentRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody BookAppointmentRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        Appointment appointment = appointmentService.book(request);
        URI location = uriBuilder.path("/api/appointments/{refRdv}")
                .buildAndExpand(appointment.getRefRdv()).toUri();
        return ResponseEntity.created(location).body(AppointmentResponse.from(appointment));
    }

    @GetMapping("/{refRdv}")
    public AppointmentResponse getByRef(@PathVariable String refRdv) {
        return AppointmentResponse.from(appointmentService.getByRef(refRdv));
    }

    @GetMapping
    public List<AppointmentResponse> list(@RequestParam(required = false) String managerRef) {
        return appointmentService.list(managerRef).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    @DeleteMapping("/{refRdv}")
    public AppointmentResponse cancel(@PathVariable String refRdv) {
        return AppointmentResponse.from(appointmentService.cancel(refRdv));
    }
}
