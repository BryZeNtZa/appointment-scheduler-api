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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppointmentServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-22T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime VALID_DATE = LocalDateTime.of(2026, 5, 25, 9, 0);

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private Department department;

    private AppointmentService service;

    private User manager;
    private User client;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, userRepository, departmentRepository, FIXED_CLOCK);

        manager = new User("MGR-1", "mgr@afb.test", "0600000001", "Doe", "John", Role.MANAGER);
        client = new User("CLI-1", "cli@afb.test", "0600000002", "Roe", "Jane", Role.CLIENT);

        when(userRepository.findByRef("MGR-1")).thenReturn(Optional.of(manager));
        when(userRepository.findByRef("CLI-1")).thenReturn(Optional.of(client));
        when(departmentRepository.findByCode("HR")).thenReturn(Optional.of(department));
        when(appointmentRepository.existsByManagerAndSlotStartAndStatus(any(), any(), any())).thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private BookAppointmentRequest request(String secondClient, LocalDateTime date) {
        return new BookAppointmentRequest(null, "CLI-1", secondClient, "HR", "MGR-1", date, "Annual review");
    }

    @Test
    void booksAppointmentForValidRequest() {
        Appointment appointment = service.book(request(null, VALID_DATE));

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(appointment.getManager().getRef()).isEqualTo("MGR-1");
        assertThat(appointment.getParticipants()).hasSize(1);
        assertThat(appointment.getSlotStart()).isEqualTo(VALID_DATE);
        verify(appointmentRepository).saveAndFlush(any(Appointment.class));
    }

    @Test
    void booksAppointmentWithTwoParticipants() {
        when(userRepository.findByRef("CLI-2")).thenReturn(Optional.of(
                new User("CLI-2", "cli2@afb.test", "0600000003", "Smith", "Sam", Role.CLIENT)));

        Appointment appointment = service.book(request("CLI-2", VALID_DATE));

        assertThat(appointment.getParticipants()).hasSize(2);
    }

    @Test
    void rejectsWhenManagerDoesNotExist() {
        when(userRepository.findByRef("MGR-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.book(request(null, VALID_DATE)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsWhenResponsableIsNotAManager() {
        when(userRepository.findByRef("MGR-1")).thenReturn(Optional.of(
                new User("MGR-1", "x@afb.test", "0", "A", "B", Role.CLIENT)));

        assertThatThrownBy(() -> service.book(request(null, VALID_DATE)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsWhenDepartmentDoesNotExist() {
        when(departmentRepository.findByCode("HR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.book(request(null, VALID_DATE)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsWhenParticipantIsNotAClient() {
        when(userRepository.findByRef("CLI-1")).thenReturn(Optional.of(
                new User("CLI-1", "x@afb.test", "0", "A", "B", Role.MANAGER)));

        assertThatThrownBy(() -> service.book(request(null, VALID_DATE)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsDuplicateParticipant() {
        BookAppointmentRequest duplicate =
                new BookAppointmentRequest(null, "CLI-1", "CLI-1", "HR", "MGR-1", VALID_DATE, "x");

        assertThatThrownBy(() -> service.book(duplicate))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsSlotThatIsNotOnTheHour() {
        assertThatThrownBy(() -> service.book(request(null, LocalDateTime.of(2026, 5, 25, 9, 30))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsSlotOutsideWorkingHours() {
        assertThatThrownBy(() -> service.book(request(null, LocalDateTime.of(2026, 5, 25, 17, 0))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsAppointmentLessThanTwoDaysInAdvance() {
        assertThatThrownBy(() -> service.book(request(null, LocalDateTime.of(2026, 5, 23, 9, 0))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsWhenManagerAlreadyBookedForSlot() {
        when(appointmentRepository.existsByManagerAndSlotStartAndStatus(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.book(request(null, VALID_DATE)))
                .isInstanceOf(ConflictException.class);
        verify(appointmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void translatesUniqueConstraintViolationToConflict() {
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.book(request(null, VALID_DATE)))
                .isInstanceOf(ConflictException.class);
    }
}
