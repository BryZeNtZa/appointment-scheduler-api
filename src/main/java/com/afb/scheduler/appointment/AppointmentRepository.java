package com.afb.scheduler.appointment;

import com.afb.scheduler.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @EntityGraph(attributePaths = {"manager", "department", "participants"})
    Optional<Appointment> findByRefRdv(String refRdv);

    boolean existsByManagerAndSlotStartAndStatus(User manager, LocalDateTime slotStart, AppointmentStatus status);

    @EntityGraph(attributePaths = {"manager", "department", "participants"})
    List<Appointment> findByManager_RefOrderByDateRdvAsc(String managerRef);

    @EntityGraph(attributePaths = {"manager", "department", "participants"})
    List<Appointment> findByStatusOrderByDateRdvAsc(AppointmentStatus status);
}
