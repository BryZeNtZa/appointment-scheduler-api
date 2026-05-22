package com.afb.scheduler.appointment;

import com.afb.scheduler.appointment.dto.BookAppointmentRequest;
import com.afb.scheduler.common.error.BusinessRuleException;
import com.afb.scheduler.common.error.ConflictException;
import com.afb.scheduler.common.error.ResourceNotFoundException;
import com.afb.scheduler.department.Department;
import com.afb.scheduler.department.DepartmentRepository;
import com.afb.scheduler.user.Role;
import com.afb.scheduler.user.User;
import com.afb.scheduler.user.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    static final int MINIMUM_DAYS_IN_ADVANCE = 2;
    static final int MAX_PARTICIPANTS = 2;

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final Clock clock;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              UserRepository userRepository,
                              DepartmentRepository departmentRepository,
                              Clock clock) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.clock = clock;
    }

    @Transactional
    public Appointment book(BookAppointmentRequest request) {
        User manager = requireUser(request.refResponsable(), Role.MANAGER, "Manager");
        Department department = departmentRepository.findByCode(request.refService())
                .orElseThrow(() -> ResourceNotFoundException.of("Department", request.refService()));
        List<User> participants = resolveParticipants(request);

        validateSlot(request.dateRdv());
        validateLeadTime(request.dateRdv());

        LocalDateTime slotStart = TimeSlot.slotStartOf(request.dateRdv());
        if (appointmentRepository.existsByManagerAndSlotStartAndStatus(manager, slotStart, AppointmentStatus.BOOKED)) {
            throw new ConflictException(
                    "Manager %s already has an appointment at %s".formatted(manager.getRef(), slotStart));
        }

        Appointment appointment = new Appointment(
                resolveRef(request.refRdv()),
                manager,
                department,
                request.dateRdv(),
                request.motifRdv()
        );
        participants.forEach(appointment::addParticipant);

        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            // Lost the race: a concurrent transaction booked the same manager+slot first.
            throw new ConflictException(
                    "Manager %s already has an appointment at %s".formatted(manager.getRef(), slotStart));
        }
    }

    @Transactional(readOnly = true)
    public Appointment getByRef(String refRdv) {
        return appointmentRepository.findByRefRdv(refRdv)
                .orElseThrow(() -> ResourceNotFoundException.of("Appointment", refRdv));
    }

    @Transactional(readOnly = true)
    public List<Appointment> list(String managerRef) {
        if (managerRef != null && !managerRef.isBlank()) {
            return appointmentRepository.findByManager_RefOrderByDateRdvAsc(managerRef);
        }
        return appointmentRepository.findByStatusOrderByDateRdvAsc(AppointmentStatus.BOOKED);
    }

    @Transactional
    public Appointment cancel(String refRdv) {
        Appointment appointment = getByRef(refRdv);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BusinessRuleException("Appointment is already cancelled: " + refRdv);
        }
        appointment.cancel();
        return appointmentRepository.save(appointment);
    }

    private List<User> resolveParticipants(BookAppointmentRequest request) {
        List<User> participants = new ArrayList<>();
        participants.add(requireUser(request.refClient(), Role.CLIENT, "Client"));

        String second = request.secondClientRef();
        if (second != null && !second.isBlank()) {
            if (second.equals(request.refClient())) {
                throw new BusinessRuleException("An appointment cannot list the same client twice.");
            }
            participants.add(requireUser(second, Role.CLIENT, "Client"));
        }

        if (participants.size() > MAX_PARTICIPANTS) {
            throw new BusinessRuleException(
                    "An appointment can include a maximum of " + MAX_PARTICIPANTS + " individuals.");
        }
        return participants;
    }

    private void validateSlot(LocalDateTime dateRdv) {
        if (!TimeSlot.isValidStart(dateRdv)) {
            throw new BusinessRuleException(
                    "Appointments must start on the hour between %02d:00 and %02d:00."
                            .formatted(TimeSlot.FIRST_START_HOUR, TimeSlot.LAST_START_HOUR));
        }
    }

    private void validateLeadTime(LocalDateTime dateRdv) {
        LocalDate earliest = LocalDate.now(clock).plusDays(MINIMUM_DAYS_IN_ADVANCE);
        if (dateRdv.toLocalDate().isBefore(earliest)) {
            throw new BusinessRuleException(
                    "An appointment must be scheduled at least " + MINIMUM_DAYS_IN_ADVANCE + " days in advance.");
        }
    }

    private User requireUser(String ref, Role expectedRole, String label) {
        User user = userRepository.findByRef(ref)
                .orElseThrow(() -> ResourceNotFoundException.of(label, ref));
        if (user.getRole() != expectedRole) {
            throw new BusinessRuleException(
                    "%s must reference a user with role %s: %s".formatted(label, expectedRole, ref));
        }
        return user;
    }

    private String resolveRef(String requestedRef) {
        if (requestedRef != null && !requestedRef.isBlank()) {
            return requestedRef;
        }
        return "RDV-" + UUID.randomUUID();
    }
}
